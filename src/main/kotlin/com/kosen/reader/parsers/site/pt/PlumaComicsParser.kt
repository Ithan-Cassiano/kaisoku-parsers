package com.kosen.reader.parsers.site.pt

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
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
import com.kosen.reader.parsers.util.attrAsRelativeUrl
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.mapChapters
import com.kosen.reader.parsers.util.nullIfEmpty
import com.kosen.reader.parsers.util.mapNotNullToSet
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.parseJson
import com.kosen.reader.parsers.util.parseJsonArray
import com.kosen.reader.parsers.util.toAbsoluteUrl
import com.kosen.reader.parsers.util.urlEncoded
import com.kosen.reader.parsers.util.json.getStringOrNull
import com.kosen.reader.parsers.util.json.mapJSONNotNull
import java.net.URLDecoder
import java.util.EnumSet
import java.util.LinkedHashSet
import java.util.Locale

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
		if (filter.hasListFilters()) {
			return fetchFilteredList(page, order, filter, query)
		}
		if (!query.isNullOrEmpty()) {
			if (page > paginator.firstPage) {
				return emptyList()
			}
			return fetchSearch(query)
		}
		return fetchCatalog(page, order)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val slug = manga.url.removePrefix("/series/").trim('/')
		val title = doc.selectFirst("h1")?.text()?.trim()?.nullIfEmpty() ?: manga.title
		val description = doc.selectFirst(".card p.text-text-secondary")?.text()?.trim()
			?: doc.selectFirst("meta[name=description]")?.attr("content")?.trim()
		val coverPath = parseCoverPath(doc) ?: parseCoverPathFromPreload(doc)
		val tags = doc.select("span.rounded-full.text-text-secondary").mapNotNullToSet { span ->
			val tagTitle = span.text().trim().nullIfEmpty() ?: return@mapNotNullToSet null
			MangaTag(
				key = tagTitle.lowercase(Locale.ROOT),
				title = tagTitle,
				source = source,
			)
		}
		val state = parseStateFromDocument(doc)
		val chapters = doc.select("a.flex.items-center.justify-between[href^=/ler/]").mapChapters { _, a ->
			if (a.selectFirst(".badge-vip") != null) {
				return@mapChapters null
			}
			val href = a.attrAsRelativeUrl("href")
			val titleText = a.selectFirst("span.text-sm.font-medium")?.text()?.trim().orEmpty()
			val number = CHAPTER_NUMBER_REGEX.find(titleText)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
			MangaChapter(
				id = generateUid(href),
				title = titleText.nullIfEmpty(),
				number = number,
				volume = 0,
				url = href,
				scanlator = null,
				uploadDate = 0,
				branch = null,
				source = source,
			)
		}
		return manga.copy(
			title = title,
			url = "/series/$slug",
			publicUrl = "https://$domain/series/$slug",
			coverUrl = coverPath?.let(::coverUrl) ?: manga.coverUrl,
			largeCoverUrl = coverPath?.let(::coverUrl) ?: manga.largeCoverUrl,
			description = description ?: manga.description,
			tags = if (tags.isNotEmpty()) tags else manga.tags,
			state = state ?: manga.state,
			contentRating = resolveContentRating(tags, manga.contentRating),
			chapters = chapters,
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

	private suspend fun fetchFilteredList(
		page: Int,
		order: SortOrder,
		filter: MangaListFilter,
		query: String?,
	): List<Manga> {
		val url = "https://$domain/api/mangas".toHttpUrl().newBuilder().apply {
			addQueryParameter("query", query.orEmpty())
			addQueryParameter("page", page.toString())
			addQueryParameter("limit", PAGE_SIZE.toString())
			addQueryParameter("orderBy", order.toApiOrderBy())

			if (filter.tags.isNotEmpty()) {
				addQueryParameter("genreIds", filter.tags.joinToString(",") { it.key })
				filter.tags.forEach { addQueryParameter("genreId", it.key) }
			}

			filter.states.firstOrNull()?.toApiSeriesStatus()?.let {
				addQueryParameter("seriesStatus", it)
			}

			filter.types.firstOrNull()?.toApiSeriesType()?.let {
				addQueryParameter("seriesType", it)
			}
		}.build()

		val response = runCatching {
			webClient.httpGet(url, getApiHeaders()).parseJson()
		}.getOrNull() ?: return fetchCatalogWithFilters(page, order, filter, query)

		val mangasArray = response.optJSONArray("mangas")
			?: response.optJSONArray("results")
			?: response.optJSONArray("data")
			?: JSONArray()
		val parsed = mangasArray.mapJSONNotNull { parseMangaFromJson(it) }
		if (parsed.isNotEmpty()) {
			return parsed
		}
		return fetchCatalogWithFilters(page, order, filter, query)
	}

	private suspend fun fetchCatalogWithFilters(
		page: Int,
		order: SortOrder,
		filter: MangaListFilter,
		query: String?,
	): List<Manga> {
		val catalog = fetchCatalog(page, order)
		return catalog.filter { manga ->
			val matchesQuery = query.isNullOrEmpty() ||
				manga.title.contains(query, ignoreCase = true)
			val matchesTags = filter.tags.isEmpty() ||
				filter.tags.any { tag -> manga.tags.any { it.title.equals(tag.title, ignoreCase = true) } }
			val matchesState = filter.states.isEmpty() ||
				(manga.state != null && manga.state in filter.states)
			matchesQuery && matchesTags && matchesState
		}
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		cachedTags?.let { return it }
		val fromApi = runCatching {
			webClient.httpGet("https://$domain/api/genres", getApiHeaders()).parseJsonArray()
		}.getOrNull()?.let { parseTagsArray(it) }
			?: runCatching {
				val json = webClient.httpGet("https://$domain/api/genres", getApiHeaders()).parseJson()
				parseTagsArray(json.optJSONArray("genres") ?: json.optJSONArray("data") ?: JSONArray())
			}.getOrNull()

		if (!fromApi.isNullOrEmpty()) {
			cachedTags = fromApi
			return fromApi
		}

		val fromCatalog = runCatching {
			val doc = webClient.httpGet("https://$domain/series").parseHtml()
			val tags = LinkedHashSet<MangaTag>()
			doc.select("span.rounded-full.text-text-secondary").forEach { span ->
				val title = span.text().trim().nullIfEmpty() ?: return@forEach
				tags.add(
					MangaTag(
						key = title.lowercase(Locale.ROOT),
						title = title,
						source = source,
					),
				)
			}
			tags
		}.getOrDefault(emptySet())

		cachedTags = fromCatalog
		return fromCatalog
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

	private fun SortOrder.toApiOrderBy(): String = when (this) {
		SortOrder.UPDATED -> "updatedAt"
		SortOrder.POPULARITY -> "views"
		SortOrder.NEWEST -> "createdAt"
		SortOrder.ALPHABETICAL,
		SortOrder.ALPHABETICAL_DESC,
		-> "title"
		else -> "updatedAt"
	}

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

	private suspend fun fetchCatalog(page: Int, order: SortOrder): List<Manga> {
		val url = "https://$domain/series".toHttpUrl().newBuilder().apply {
			if (page > paginator.firstPage) {
				addQueryParameter("page", page.toString())
			}
			when (order) {
				SortOrder.UPDATED -> addQueryParameter("sort", "updated")
				else -> Unit
			}
		}.build()
		val doc = webClient.httpGet(url.toString()).parseHtml()
		return parseCatalog(doc)
	}

	private fun parseCatalog(doc: Document): List<Manga> {
		return doc.select("a.group.block[href^=/series/]").mapNotNull { a ->
			val href = a.attrAsRelativeUrl("href")
			val slug = href.removePrefix("/series/").trim('/').nullIfEmpty() ?: return@mapNotNull null
			val title = a.selectFirst("h3")?.text()?.trim()?.nullIfEmpty()
				?: a.selectFirst("img[alt]")?.attr("alt")?.trim()?.nullIfEmpty()
				?: return@mapNotNull null
			val coverPath = parseCoverPath(a)
			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = "https://$domain/series/$slug",
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.SAFE,
				coverUrl = coverPath?.let(::coverUrl),
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				largeCoverUrl = null,
				description = null,
				source = source,
			)
		}
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

	private fun parseCoverPath(element: Element): String? {
		val raw = element.selectFirst("img[srcset], img[srcSet]")?.attr("srcSet")
			?: element.selectFirst("img[src]")?.attr("src")
			?: return null
		return extractCoverPath(raw)
	}

	private fun parseCoverPathFromPreload(doc: Document): String? {
		val preload = doc.selectFirst("link[rel=preload][as=image]")?.attr("href") ?: return null
		return extractCoverPath(preload)
	}

	private fun extractCoverPath(raw: String): String? {
		val decoded = URLDecoder.decode(raw, Charsets.UTF_8.name())
		val match = COVER_PATH_REGEX.find(decoded) ?: return null
		return URLDecoder.decode(match.groupValues[1], Charsets.UTF_8.name()).nullIfEmpty()
	}

	private fun coverUrl(coverPath: String): String {
		return "https://$domain/api/cover/${coverPath.urlEncoded()}"
	}

	private fun parseStateFromDocument(doc: Document): MangaState? {
		val badge = doc.select("span.uppercase").firstOrNull { span ->
			val text = span.text().trim()
			text.equals("Ongoing", true) ||
				text.equals("Completo", true) ||
				text.equals("Hiato", true) ||
				text.contains("andamento", true) ||
				text == "Fim"
		}?.text()
		return parseState(badge)
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
		private const val PAGE_SIZE = 25
		private val CHAPTER_NUMBER_REGEX = Regex("""(\d+(?:\.\d+)?)""")
		private val COVER_PATH_REGEX = Regex("""/api/cover/([^?&"]+)""")
	}
}
