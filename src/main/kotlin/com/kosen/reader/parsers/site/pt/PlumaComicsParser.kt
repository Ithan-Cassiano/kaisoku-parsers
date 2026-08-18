package com.kosen.reader.parsers.site.pt

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import com.kosen.reader.parsers.MangaLoaderContext
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
import com.kosen.reader.parsers.util.nullIfEmpty
import com.kosen.reader.parsers.util.parseJson
import com.kosen.reader.parsers.util.parseSafe
import com.kosen.reader.parsers.util.toAbsoluteUrl
import com.kosen.reader.parsers.util.urlEncoded
import com.kosen.reader.parsers.util.json.getStringOrNull
import com.kosen.reader.parsers.util.json.mapJSONNotNull
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.Locale
import java.util.TimeZone

internal open class PlumaComicsParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
	defaultDomain: String,
) : PagedMangaParser(context, source, PAGE_SIZE) {

	@Volatile
	private var cachedTags: Set<MangaTag>? = null

	override val configKeyDomain = ConfigKey.Domain(defaultDomain)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override fun getRequestHeaders(): Headers = super.getRequestHeaders().newBuilder()
		.add("Referer", "https://$domain/")
		.add("Origin", "https://$domain")
		.build()

	private fun getApiHeaders(): Headers = getRequestHeaders().newBuilder()
		.set("Accept", "application/json, text/plain, */*")
		.build()

	override val defaultSortOrder: SortOrder
		get() = SortOrder.POPULARITY

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.POPULARITY,
		SortOrder.UPDATED,
		SortOrder.RELEVANCE,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
			isMultipleTagsSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions(
		availableTags = fetchAvailableTags(),
		availableStates = EnumSet.of(
			MangaState.ONGOING,
			MangaState.FINISHED,
			MangaState.PAUSED,
		),
		availableContentTypes = EnumSet.of(
			ContentType.MANGA,
			ContentType.MANHWA,
			ContentType.MANHUA,
			ContentType.COMICS,
		),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val query = filter.query?.trim()?.nullIfEmpty()
		if (filter.hasListFilters() || !query.isNullOrEmpty()) {
			return fetchObras(page, order, filter, query)
		}
		return fetchObras(page, order, filter, null)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val slug = manga.url.removePrefix("/series/").trim('/')
		val series = fetchSeriesJson(slug, manga.title) ?: return manga.copy(
			chapters = fetchChapters(slug),
		)
		val parsed = parseMangaFromJson(series) ?: manga
		return parsed.copy(
			url = "/series/$slug",
			publicUrl = "https://$domain/series/$slug",
			chapters = fetchChapters(slug),
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterId = chapter.url.removePrefix("/ler/").substringBefore('/')
		val response = webClient.httpGet(
			"https://$domain/api/viewer/bootstrap?c=$chapterId",
			getApiHeaders(),
		).parseJson()
		val pages = response.optJSONArray("pages") ?: return emptyList()
		return List(pages.length()) { index -> pages.getJSONObject(index) }
			.sortedBy { it.optInt("i", Int.MAX_VALUE) }
			.map { page ->
				val imageUrl = page.getString("u").toAbsoluteUrl(domain)
				MangaPage(
					id = generateUid("${chapterId}_${page.optInt("i", 0)}_$imageUrl"),
					url = imageUrl,
					preview = null,
					source = source,
				)
			}
	}

	private suspend fun fetchSearch(query: String): List<Manga> {
		val url = "https://$domain/api/search?q=${query.urlEncoded()}"
		val response = webClient.httpGet(url, getApiHeaders()).parseJson()
		val results = response.optJSONArray("results") ?: JSONArray()
		return results.mapJSONNotNull { parseMangaFromJson(it) }
	}

	private suspend fun fetchObras(
		page: Int,
		order: SortOrder,
		filter: MangaListFilter,
		query: String?,
	): List<Manga> {
		val url = "https://$domain/api/obras".toHttpUrl().newBuilder().apply {
			addQueryParameter("page", page.toString())
			query?.let { addQueryParameter("query", it) }
			when (order) {
				SortOrder.UPDATED -> addQueryParameter("sort", "updated")
				else -> Unit
			}
			filter.tags.firstOrNull()?.key?.let { addQueryParameter("genreId", it) }
			filter.states.firstOrNull()?.toApiSeriesStatus()?.let {
				addQueryParameter("status", it)
			}
			filter.types.firstOrNull()?.toApiSeriesType()?.let {
				addQueryParameter("seriesType", it)
			}
		}.build()
		val response = webClient.httpGet(url, getApiHeaders()).parseJson()
		cachedTags = cachedTags ?: parseTagsArray(
			response.optJSONArray("genres") ?: JSONArray(),
		).takeIf { it.isNotEmpty() }
		val mangasArray = response.optJSONArray("series")
			?: response.optJSONArray("results")
			?: response.optJSONArray("data")
			?: JSONArray()
		val parsed = mangasArray.mapJSONNotNull { parseMangaFromJson(it) }
		if (parsed.isEmpty() && !query.isNullOrEmpty() && page == paginator.firstPage) {
			return fetchSearch(query)
		}
		return parsed
	}

	private suspend fun fetchSeriesJson(slug: String, title: String): JSONObject? {
		val queries = linkedSetOf(
			title.trim(),
			slug.replace('-', ' '),
			slug,
		).filter { it.isNotBlank() }
		for (query in queries) {
			findSeriesBySlug(
				webClient.httpGet(
					"https://$domain/api/search?q=${query.urlEncoded()}",
					getApiHeaders(),
				).parseJson().optJSONArray("results") ?: JSONArray(),
				slug,
			)?.let { return it }
			findSeriesBySlug(
				webClient.httpGet(
					"https://$domain/api/obras?query=${query.urlEncoded()}&page=1",
					getApiHeaders(),
				).parseJson().optJSONArray("series") ?: JSONArray(),
				slug,
			)?.let { return it }
		}
		return null
	}

	private fun findSeriesBySlug(array: JSONArray, slug: String): JSONObject? {
		for (i in 0 until array.length()) {
			val obj = array.optJSONObject(i) ?: continue
			if (obj.optString("slug") == slug) {
				return obj
			}
		}
		return null
	}

	private suspend fun fetchChapters(slug: String): List<MangaChapter> {
		// /api/manga/{id}/chapters exige sessão e o httpGet trata 401 como erro de rede.
		val updates = webClient.httpGet(
			"https://$domain/api/latest-updates?take=60",
			getApiHeaders(),
		).parseJson()
		val items = updates.optJSONArray("items") ?: return emptyList()
		for (i in 0 until items.length()) {
			val item = items.optJSONObject(i) ?: continue
			if (item.optJSONObject("series")?.optString("slug") != slug) continue
			return parseChapterList(item.optJSONArray("chapters") ?: JSONArray())
		}
		return emptyList()
	}

	private fun parseChapterList(array: JSONArray): List<MangaChapter> {
		return array.mapJSONNotNull { json ->
			if (json.optBoolean("isVipOnly", false)) {
				return@mapJSONNotNull null
			}
			val id = json.opt("id")?.toString()?.nullIfEmpty() ?: return@mapJSONNotNull null
			val number = json.opt("number")?.toString()?.toFloatOrNull() ?: 0f
			val href = "/ler/$id"
			MangaChapter(
				id = generateUid(href),
				title = null,
				number = number,
				volume = 0,
				url = href,
				scanlator = null,
				uploadDate = isoDateFormat.parseSafe(
					json.getStringOrNull("publishedAt")?.substringBefore('.')?.substringBefore('Z'),
				),
				branch = null,
				source = source,
			)
		}.sortedBy { it.number }
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		cachedTags?.let { return it }
		val fromObras = runCatching {
			val json = webClient.httpGet("https://$domain/api/obras?page=1", getApiHeaders()).parseJson()
			parseTagsArray(json.optJSONArray("genres") ?: JSONArray())
		}.getOrNull()
		if (!fromObras.isNullOrEmpty()) {
			cachedTags = fromObras
			return fromObras
		}
		cachedTags = emptySet()
		return emptySet()
	}

	private fun parseTagsArray(array: JSONArray): Set<MangaTag> {
		return array.mapJSONNotNull { json ->
			val key = json.opt("id")?.toString()?.trim()?.nullIfEmpty()
				?: json.getStringOrNull("slug")?.trim()?.nullIfEmpty()
				?: json.getStringOrNull("name")?.trim()?.lowercase(Locale.ROOT)
			val title = json.getStringOrNull("name")
				?: json.getStringOrNull("title")
			if (key.isNullOrEmpty() || title.isNullOrBlank()) {
				null
			} else {
				MangaTag(key = key, title = title, source = source)
			}
		}.toSet()
	}

	private fun MangaListFilter.hasListFilters(): Boolean =
		tags.isNotEmpty() || states.isNotEmpty() || types.isNotEmpty()

	private fun MangaState.toApiSeriesStatus(): String? = when (this) {
		MangaState.ONGOING -> "ongoing"
		MangaState.FINISHED -> "completed"
		MangaState.PAUSED -> "hiatus"
		else -> null
	}

	private fun ContentType.toApiSeriesType(): String? = when (this) {
		ContentType.MANGA -> "manga"
		ContentType.MANHWA -> "manhwa"
		ContentType.MANHUA -> "manhua"
		ContentType.COMICS -> "comic"
		else -> null
	}

	private fun parseMangaFromJson(json: JSONObject): Manga? {
		val slug = json.getStringOrNull("slug") ?: return null
		val href = "/series/$slug"
		val tags = json.optJSONArray("genres")?.let { array ->
			(0 until array.length()).mapNotNull { index ->
				val title = array.optString(index).trim().nullIfEmpty() ?: return@mapNotNull null
				MangaTag(
					key = title.lowercase(Locale.ROOT),
					title = title,
					source = source,
				)
			}.toSet()
		}.orEmpty()
		val coverPath = json.getStringOrNull("coverPath")
		return Manga(
			id = generateUid(json.optInt("id", 0).takeIf { it > 0 }?.toString() ?: href),
			title = json.getStringOrNull("title").orEmpty(),
			altTitles = emptySet(),
			url = href,
			publicUrl = "https://$domain/series/$slug",
			rating = RATING_UNKNOWN,
			contentRating = resolveContentRating(tags, null),
			coverUrl = coverPath?.let(::coverUrl),
			tags = tags,
			state = parseState(json.getStringOrNull("status")),
			authors = emptySet(),
			largeCoverUrl = null,
			description = null,
			source = source,
		)
	}

	private fun coverUrl(coverPath: String): String {
		val path = coverPath.removePrefix("/")
		return if (path.startsWith("http://") || path.startsWith("https://")) {
			path
		} else {
			"https://$domain/api/img/$path"
		}
	}

	private fun parseState(raw: String?): MangaState? = when (raw?.trim()?.lowercase(Locale.ROOT)) {
		"ongoing", "em andamento" -> MangaState.ONGOING
		"completed", "completo", "fim" -> MangaState.FINISHED
		"hiatus", "hiato" -> MangaState.PAUSED
		else -> null
	}

	private fun resolveContentRating(tags: Set<MangaTag>, fallback: ContentRating?): ContentRating {
		if (fallback == ContentRating.ADULT) {
			return ContentRating.ADULT
		}
		val explicitAdultKeywords = setOf("hentai", "+18", "18+", "adulto", "adult", "nsfw", "r18", "r-18")
		val isExplicitAdult = tags.any { tag ->
			val key = tag.title.lowercase(Locale.ROOT)
			explicitAdultKeywords.any { keyword -> key == keyword || key.contains(keyword) }
		}
		return if (isExplicitAdult) ContentRating.ADULT else ContentRating.SAFE
	}

	private companion object {
		private const val PAGE_SIZE = 30
		private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
			timeZone = TimeZone.getTimeZone("UTC")
		}
	}
}
