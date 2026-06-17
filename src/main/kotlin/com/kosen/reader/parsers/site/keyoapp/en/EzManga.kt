package com.kosen.reader.parsers.site.keyoapp.en

import org.json.JSONArray
import org.json.JSONObject
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.ContentRating
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaListFilter
import com.kosen.reader.parsers.model.MangaListFilterOptions
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.model.MangaState
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.model.RATING_UNKNOWN
import com.kosen.reader.parsers.model.SortOrder
import com.kosen.reader.parsers.site.iken.IkenParser
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.mapChapters
import com.kosen.reader.parsers.util.parseJson
import com.kosen.reader.parsers.util.parseJsonArray
import com.kosen.reader.parsers.util.parseSafe
import com.kosen.reader.parsers.util.toAbsoluteUrl
import com.kosen.reader.parsers.util.urlEncoded
import com.kosen.reader.parsers.util.json.asTypedList
import com.kosen.reader.parsers.util.json.getBooleanOrDefault
import com.kosen.reader.parsers.util.json.getFloatOrDefault
import com.kosen.reader.parsers.util.json.getLongOrDefault
import com.kosen.reader.parsers.util.json.getStringOrNull
import com.kosen.reader.parsers.util.json.mapJSONNotNull
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.Locale

@MangaSourceParser("EZMANGA", "EzManga", "en")
internal class EzManga(context: MangaLoaderContext) :
	IkenParser(context, MangaParserSource.EZMANGA, "ezmanga.org", 18, useAPI = true) {

	private val apiDomain = "vapi.ezmanga.org"
	private val apiUrl = "https://$apiDomain/api/v1"

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.POPULARITY,
		SortOrder.UPDATED,
		SortOrder.NEWEST,
		SortOrder.ALPHABETICAL_DESC,
	)

	override suspend fun getFilterOptions(): MangaListFilterOptions = super.getFilterOptions().copy(
		availableStates = EnumSet.of(
			MangaState.ONGOING,
			MangaState.PAUSED,
			MangaState.FINISHED,
			MangaState.ABANDONED,
		),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val query = filter.query?.trim().orEmpty()
		val url = if (query.isNotEmpty()) {
			"$apiUrl/series/search?page=$page&perPage=20&q=${query.urlEncoded()}"
		} else {
			val sort = when (order) {
				SortOrder.POPULARITY -> "popular"
				SortOrder.NEWEST -> "newest"
				SortOrder.ALPHABETICAL_DESC -> "title"
				else -> "latest"
			}
			buildString {
				append("$apiUrl/series?page=$page&perPage=20&sort=$sort")
				filter.states.firstOrNull()?.toApiSeriesStatus()?.let {
					append("&status=$it")
				}
				filter.types.firstOrNull()?.toApiSeriesType()?.let {
					append("&type=$it")
				}
				filter.tags.firstOrNull()?.key?.takeIf { it.isNotBlank() }?.let {
					append("&genre=${it.urlEncoded()}")
				}
			}
		}
		return parseMangaList(webClient.httpGet(url).parseJson())
	}

	override fun parseMangaList(json: JSONObject): List<Manga> {
		val posts = json.optJSONArray("posts")
			?: json.optJSONArray("data")
			?: json.optJSONObject("data")?.optJSONArray("posts")
			?: json.optJSONObject("result")?.optJSONArray("posts")
			?: return emptyList()
		return posts.mapJSONNotNull { post ->
			val slug = post.getStringOrNull("slug") ?: return@mapJSONNotNull null
			val url = "/series/$slug"
			val isAdult = post.getBooleanOrDefault("hot", false) || post.getBooleanOrDefault("isAdult", false)
			val state = parseState(post.getStringOrNull("seriesStatus") ?: post.getStringOrNull("status"))
			Manga(
				id = post.getLongOrDefault("id", generateUid(url)),
				url = url,
				publicUrl = url.toAbsoluteUrl(domain),
				coverUrl = post.getStringOrNull("featuredImage")
					?: post.getStringOrNull("cover"),
				title = post.getStringOrNull("postTitle")
					?: post.getStringOrNull("title")
					?: slug,
				altTitles = setOfNotNull(post.getStringOrNull("alternativeTitles")),
				description = post.getStringOrNull("postContent")
					?: post.getStringOrNull("description"),
				rating = RATING_UNKNOWN,
				tags = emptySet(),
				authors = setOfNotNull(post.getStringOrNull("author")),
				state = state,
				source = source,
				contentRating = if (isAdult) ContentRating.ADULT else null,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val slug = manga.url.substringAfterLast('/')
		val post = webClient.httpGet("$apiUrl/series/$slug").parseJson()
		val dateFormat = SimpleDateFormat(datePattern, Locale.ENGLISH)
		val chapters = mutableListOf<JSONObject>()
		var page = 1
		while (true) {
			val chaptersResponse = webClient.httpGet(
				"$apiUrl/series/$slug/chapters?page=$page&perPage=30&sort=desc",
			).parseJson()
			val data = chaptersResponse.optJSONArray("data") ?: JSONArray()
			if (data.length() == 0) break
			chapters += data.asTypedList()
			val totalPages = chaptersResponse.optInt("totalPages", page)
			if (page >= totalPages) break
			page++
		}

		val parsedChapters = chapters.mapChapters(reversed = true) { i, ch ->
			val chapterSlug = ch.getStringOrNull("slug")
				?: ch.getStringOrNull("chapterSlug")
				?: return@mapChapters null
			val chapterUrl = "/series/$slug/$chapterSlug"
			val chapterNumber = ch.getFloatOrDefault("number", i + 1f)
			val chapterTitle = ch.getStringOrNull("title")
				?.takeIf { it.isNotBlank() }
				?: "Chapter ${chapterNumber.toInt().takeIf { it > 0 } ?: chapterNumber}"
			MangaChapter(
				id = ch.getLongOrDefault("id", generateUid(chapterUrl)),
				title = chapterTitle,
				number = chapterNumber,
				volume = 0,
				url = chapterUrl,
				scanlator = null,
				uploadDate = dateFormat.parseSafe(ch.getStringOrNull("createdAt")?.substringBefore("T")),
				branch = null,
				source = source,
			)
		}

		val tags = (post.optJSONArray("genres") ?: JSONArray()).mapJSONNotNull { item ->
			val key = item.getStringOrNull("slug")
				?: item.getStringOrNull("name")?.toApiGenreKey()
				?: item.opt("id")?.toString()
			val title = item.getStringOrNull("name") ?: return@mapJSONNotNull null
			if (key.isNullOrBlank()) {
				null
			} else {
				MangaTag(
					key = key,
					title = title,
					source = source,
				)
			}
		}.toSet()

		return manga.copy(
			title = post.getStringOrNull("postTitle") ?: post.getStringOrNull("title") ?: manga.title,
			coverUrl = post.getStringOrNull("featuredImage") ?: post.getStringOrNull("cover") ?: manga.coverUrl,
			description = post.getStringOrNull("postContent") ?: post.getStringOrNull("description"),
			altTitles = setOfNotNull(post.getStringOrNull("alternativeTitles")),
			state = parseState(post.getStringOrNull("seriesStatus") ?: post.getStringOrNull("status")),
			authors = setOfNotNull(post.getStringOrNull("author"), post.getStringOrNull("artist")),
			tags = tags,
			chapters = parsedChapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterPath = chapter.url.substringBefore('?').trim('/')
		val chapterEndpoint = if (chapterPath.contains("/chapters/")) {
			"$apiUrl/$chapterPath"
		} else {
			val parts = chapterPath.split('/')
			if (parts.size >= 3 && parts[0] == "series") {
				"$apiUrl/series/${parts[1]}/chapters/${parts[2]}"
			} else {
				return super.getPages(chapter)
			}
		}

		val response = webClient.httpGet(chapterEndpoint).parseJson()
		val chapterObj = response.optJSONObject("chapter")
			?: response.optJSONObject("data")
			?: response
		if (chapterObj.optBoolean("isLocked", false) || response.optBoolean("isLocked", false)) {
			throw Exception("Need to unlock chapter!")
		}

		val images = extractImages(chapterObj.optJSONArray("images"))
			.ifEmpty { extractImages(response.optJSONArray("images")) }
			.ifEmpty { extractImages(response.optJSONObject("data")?.optJSONArray("images")) }

		if (images.isEmpty()) {
			return super.getPages(chapter)
		}

		return images.map { imageUrl ->
			MangaPage(
				id = generateUid(imageUrl),
				url = imageUrl,
				preview = null,
				source = source,
			)
		}
	}

	override suspend fun fetchAvailableTags(): Set<MangaTag> {
		val fromArray = runCatching {
			webClient.httpGet("$apiUrl/genres")
				.parseJsonArray()
				.toGenreTags()
		}.getOrNull().orEmpty()
		if (fromArray.isNotEmpty()) {
			return fromArray
		}

		val fromObject = runCatching {
			val obj = webClient.httpGet("$apiUrl/genres").parseJson()
			(obj.optJSONArray("genres") ?: obj.optJSONArray("data") ?: JSONArray()).toGenreTags()
		}.getOrNull().orEmpty()
		if (fromObject.isNotEmpty()) {
			return fromObject
		}

		return super.fetchAvailableTags()
	}

	private fun JSONArray.toGenreTags(): Set<MangaTag> = mapJSONNotNull { item ->
		val key = item.getStringOrNull("slug")
			?: item.getStringOrNull("name")?.toApiGenreKey()
			?: item.opt("id")?.toString()?.trim().orEmpty()
		val title = item.getStringOrNull("name")
			?: item.getStringOrNull("title")
		if (key.isEmpty() || title.isNullOrBlank()) {
			null
		} else {
			MangaTag(
				key = key,
				title = title,
				source = source,
			)
		}
	}.toSet()

	private fun MangaState.toApiSeriesStatus(): String? = when (this) {
		MangaState.ONGOING -> "ONGOING"
		MangaState.PAUSED -> "HIATUS"
		MangaState.FINISHED -> "COMPLETED"
		MangaState.ABANDONED -> "DROPPED"
		else -> null
	}

	private fun ContentType.toApiSeriesType(): String? = when (this) {
		ContentType.MANGA -> "MANGA"
		ContentType.MANHWA -> "MANHWA"
		ContentType.MANHUA -> "MANHUA"
		ContentType.OTHER -> "RUSSIAN"
		else -> null
	}

	private fun parseState(status: String?): MangaState? = when (status?.uppercase(Locale.ROOT)) {
		"ONGOING", "MASS_RELEASED" -> MangaState.ONGOING
		"HIATUS" -> MangaState.PAUSED
		"COMPLETED" -> MangaState.FINISHED
		"DROPPED", "CANCELLED" -> MangaState.ABANDONED
		"COMING_SOON" -> MangaState.UPCOMING
		else -> null
	}

	private fun extractImages(array: JSONArray?): List<String> {
		if (array == null) return emptyList()
		return (0 until array.length()).mapNotNull { index ->
			when (val item = array.opt(index)) {
				is String -> item.takeIf { it.isNotBlank() }
				is JSONObject -> item.getStringOrNull("url")
					?: item.getStringOrNull("src")
					?: item.getStringOrNull("image")
				else -> null
			}
		}
	}

	private fun String.toApiGenreKey(): String = lowercase(Locale.ROOT)
		.replace("[^a-z0-9]+".toRegex(), "-")
		.trim('-')
}
