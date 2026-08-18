package com.kosen.reader.parsers.site.pt

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.core.PagedMangaParser
import com.kosen.reader.parsers.model.ContentRating
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaListFilter
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaListFilterOptions
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaState
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.model.RATING_UNKNOWN
import com.kosen.reader.parsers.model.SortOrder
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.json.getBooleanOrDefault
import com.kosen.reader.parsers.util.json.getFloatOrDefault
import com.kosen.reader.parsers.util.json.getStringOrNull
import com.kosen.reader.parsers.util.json.mapJSONNotNull
import com.kosen.reader.parsers.util.json.toStringSet
import com.kosen.reader.parsers.util.nullIfEmpty
import com.kosen.reader.parsers.util.parseJson
import com.kosen.reader.parsers.util.parseSafe
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.LinkedHashSet
import java.util.Locale
import java.util.TimeZone

@MangaSourceParser("GEASSCOMICS", "Geass Comics", "pt")
internal class GeassComics(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.GEASSCOMICS, PAGE_SIZE) {

	override val configKeyDomain = ConfigKey.Domain("geasscomics.xyz")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override fun getRequestHeaders(): Headers = super.getRequestHeaders().newBuilder()
		.add("Referer", "https://$domain/")
		.add("Origin", "https://$domain")
		.add("Accept", "application/json")
		.build()

	override val defaultSortOrder: SortOrder
		get() = SortOrder.UPDATED

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.NEWEST,
		SortOrder.ALPHABETICAL,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions(
		availableStates = EnumSet.of(
			MangaState.ONGOING,
			MangaState.FINISHED,
			MangaState.PAUSED,
			MangaState.ABANDONED,
		),
		availableContentRating = EnumSet.of(ContentRating.SAFE, ContentRating.ADULT),
		availableContentTypes = EnumSet.of(
			ContentType.MANGA,
			ContentType.MANHWA,
			ContentType.MANHUA,
			ContentType.COMICS,
		),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = "$API_URL/api/works".toHttpUrl().newBuilder().apply {
			addQueryParameter("page", page.toString())
			addQueryParameter("limit", PAGE_SIZE.toString())
			filter.query?.trim()?.nullIfEmpty()?.let { addQueryParameter("q", it) }
			when (order) {
				SortOrder.POPULARITY -> {
					addQueryParameter("sort", "views")
					addQueryParameter("order", "desc")
				}
				SortOrder.NEWEST -> {
					addQueryParameter("sort", "createdAt")
					addQueryParameter("order", "desc")
				}
				SortOrder.ALPHABETICAL -> {
					addQueryParameter("sort", "title")
					addQueryParameter("order", "asc")
				}
				else -> {
					addQueryParameter("sort", "updatedAt")
					addQueryParameter("order", "desc")
				}
			}
			when (filter.states.firstOrNull()) {
				MangaState.ONGOING -> addQueryParameter("status", "ongoing")
				MangaState.FINISHED -> addQueryParameter("status", "completed")
				MangaState.PAUSED -> addQueryParameter("status", "hiatus")
				MangaState.ABANDONED -> addQueryParameter("status", "cancelled")
				else -> Unit
			}
			when (filter.types.firstOrNull()) {
				ContentType.MANGA -> addQueryParameter("kind", "manga")
				ContentType.MANHWA -> addQueryParameter("kind", "manhwa")
				ContentType.MANHUA -> addQueryParameter("kind", "manhua")
				ContentType.COMICS -> addQueryParameter("kind", "comic")
				else -> Unit
			}
			when (filter.contentRating.singleOrNull()) {
				ContentRating.ADULT -> addQueryParameter("nsfw", "true")
				ContentRating.SAFE -> addQueryParameter("nsfw", "false")
				else -> Unit
			}
		}.build()
		val response = webClient.httpGet(url, getRequestHeaders()).parseJson()
		val items = response.optJSONObject("data")?.optJSONArray("items") ?: JSONArray()
		return items.mapJSONNotNull { parseWork(it) }
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val slug = manga.url.trimEnd('/').substringAfterLast('/')
		val response = webClient.httpGet("$API_URL/api/works/$slug", getRequestHeaders()).parseJson()
		val data = response.optJSONObject("data") ?: return manga
		val parsed = parseWork(data) ?: manga
		val chapters = data.optJSONArray("chapters")
			?.mapJSONNotNull { parseChapter(it, slug) }
			.orEmpty()
			.sortedBy { it.number }
		return parsed.copy(
			description = data.getStringOrNull("synopsis") ?: manga.description,
			authors = data.getStringOrNull("author")?.takeIf { it.isNotBlank() }?.let { setOf(it) }
				?: manga.authors,
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val (slug, number) = parseChapterRef(chapter.url)
		val response = webClient.httpGet(
			"$API_URL/api/works/$slug/chapters/$number",
			getRequestHeaders(),
		).parseJson()
		val data = response.optJSONObject("data") ?: return emptyList()
		val pages = data.optJSONArray("pages") ?: return emptyList()
		return List(pages.length()) { index ->
			val imageUrl = pages.optString(index)
			MangaPage(
				id = generateUid("$slug/$number/$index"),
				url = imageUrl,
				preview = null,
				source = source,
			)
		}
	}

	private fun parseWork(json: JSONObject): Manga? {
		val slug = json.getStringOrNull("slug") ?: return null
		val relativeUrl = "/work/$slug"
		val tags = json.optJSONArray("tags")?.toStringSet().orEmpty().mapTo(LinkedHashSet()) { name ->
			MangaTag(key = name.lowercase(Locale.ROOT), title = name, source = source)
		}
		val rating = json.getFloatOrDefault("rating", 0f)
		return Manga(
			id = generateUid(json.getStringOrNull("id") ?: relativeUrl),
			title = json.getStringOrNull("title").orEmpty(),
			altTitles = emptySet(),
			url = relativeUrl,
			publicUrl = "https://$domain/work/$slug",
			rating = if (rating <= 0f) RATING_UNKNOWN else (rating / 5f).coerceIn(0f, 1f),
			contentRating = if (json.getBooleanOrDefault("isNsfw", false)) {
				ContentRating.ADULT
			} else {
				ContentRating.SAFE
			},
			coverUrl = json.getStringOrNull("cover"),
			tags = tags,
			state = parseState(json.getStringOrNull("status")),
			authors = emptySet(),
			largeCoverUrl = json.getStringOrNull("cover"),
			description = json.getStringOrNull("synopsis"),
			source = source,
		)
	}

	private fun parseChapter(json: JSONObject, slug: String): MangaChapter? {
		val number = json.opt("number")?.toString()?.toFloatOrNull() ?: return null
		val numberKey = chapterNumberKey(number)
		return MangaChapter(
			id = generateUid(json.getStringOrNull("id") ?: "$slug/$numberKey"),
			title = null,
			number = number,
			volume = 0,
			url = "/work/$slug/$numberKey",
			scanlator = null,
			uploadDate = isoDateFormat.parseSafe(
				json.getStringOrNull("releasedAt")?.substringBefore('.')?.substringBefore('Z'),
			),
			branch = null,
			source = source,
		)
	}

	private fun parseChapterRef(url: String): Pair<String, String> {
		val parts = url.trim('/').split('/')
		return when {
			parts.size >= 3 && parts[0] == "work" -> parts[1] to parts[2]
			parts.size >= 4 && parts[0] == "chapter" -> parts[2] to parts.last()
			else -> url.substringAfterLast('/') to "1"
		}
	}

	private fun parseState(status: String?): MangaState? = when (status?.trim()?.lowercase(Locale.ROOT)) {
		"ongoing" -> MangaState.ONGOING
		"completed", "complete" -> MangaState.FINISHED
		"hiatus", "on_hold", "on hold" -> MangaState.PAUSED
		"cancelled", "canceled", "dropped" -> MangaState.ABANDONED
		else -> null
	}

	private fun chapterNumberKey(number: Float): String {
		val asInt = number.toInt()
		return if (number == asInt.toFloat()) asInt.toString() else number.toString()
	}

	private companion object {
		private const val PAGE_SIZE = 24
		private const val API_URL = "https://api.geasscomics.xyz"
		private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
			timeZone = TimeZone.getTimeZone("UTC")
		}
	}
}
