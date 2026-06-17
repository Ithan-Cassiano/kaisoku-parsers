package com.kosen.reader.parsers.site.madara.pt

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.*
import java.text.SimpleDateFormat
import java.util.EnumSet

@MangaSourceParser("INKAPK", "INKAPK", "pt", ContentType.HENTAI)
internal class Inkapk(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.INKAPK, "inkapk.net", pageSize = 24) {

	override val listUrl = "obras/"
	override val tagPrefix = "obras-genre/"
	override val datePattern = "dd/MM/yyyy"

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.POPULARITY,
		SortOrder.UPDATED,
		SortOrder.NEWEST,
		SortOrder.ALPHABETICAL,
		SortOrder.RATING,
		SortOrder.RELEVANCE,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = false,
			isMultipleTagsSupported = false,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions(
		availableTags = fetchAvailableTags(),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildListUrl(page, order, filter)
		return parseInkCards(webClient.httpGet(url).parseHtml())
	}

	override suspend fun fetchAvailableTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain/$listUrl").parseHtml()
		return doc.select("a[href*=/obras-genre/]").mapNotNullToSet { a ->
			val href = a.attr("href")
			val key = href.removeSuffix("/").substringAfterLast('/')
			if (key.isEmpty() || key == "obras-genre") return@mapNotNullToSet null
			MangaTag(
				key = key,
				title = a.text().ifEmpty { return@mapNotNullToSet null }.toTitleCase(sourceLocale),
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val dateFormat = SimpleDateFormat(datePattern, sourceLocale)
		return manga.copy(
			title = doc.selectFirst("h1.ink-det-title")?.text()?.trim().orEmpty().ifEmpty { manga.title },
			coverUrl = doc.selectFirst(".ink-det-cover img")?.src()
				?: doc.selectFirst("meta[property=og:image]")?.attr("content"),
			description = doc.selectFirst("p.ink-det-desc")?.html()
				?: doc.selectFirst(".description-summary .summary__content")?.html(),
			tags = doc.select(".ink-det-genres a, .genres-content a").mapNotNullToSet { a ->
				val key = a.attr("href").removeSuffix("/").substringAfterLast('/')
				if (key.isEmpty()) return@mapNotNullToSet null
				MangaTag(key = key, title = a.text().toTitleCase(sourceLocale), source = source)
			},
			state = when {
				doc.selectFirst(".ink-st-completo, .ink-st-complete") != null -> MangaState.FINISHED
				doc.selectFirst(".ink-st-ongoing") != null -> MangaState.ONGOING
				else -> null
			},
			chapters = doc.select("#ink-ch-list a.ink-ch-item").mapChapters(reversed = true) { i, a ->
				val href = a.attrAsRelativeUrl("href")
				val title = a.selectFirst(".ink-ch-item-name")?.text()?.trim()
					?: a.text().trim()
				val dateText = a.selectFirst(".ink-ch-item-date")?.text()
				MangaChapter(
					id = generateUid(href),
					title = title,
					number = i + 1f,
					volume = 0,
					url = href,
					scanlator = null,
					uploadDate = parseChapterDate(dateFormat, dateText),
					branch = null,
					source = source,
				)
			},
			contentRating = ContentRating.ADULT,
		)
	}

	private fun buildListUrl(page: Int, order: SortOrder, filter: MangaListFilter): String {
		val pageNum = page + 1
		val sort = order.toInkSort()

		if (!filter.query.isNullOrEmpty()) {
			return "https://$domain/".toHttpUrl().newBuilder()
				.addQueryParameter("s", filter.query)
				.addQueryParameter("post_type", "wp-manga")
				.apply {
					if (pageNum > 1) {
						addQueryParameter("paged", pageNum.toString())
					}
					sort?.let { addQueryParameter("sort", it) }
				}
				.build()
				.toString()
		}

		val basePath = if (filter.tags.isNotEmpty()) {
			"${tagPrefix}${filter.tags.oneOrThrowIfMany()?.key}/"
		} else {
			listUrl
		}
		val path = if (pageNum > 1) {
			basePath.trimEnd('/') + "/page/$pageNum/"
		} else {
			basePath
		}

		return "https://$domain/$path".toHttpUrl().newBuilder()
			.apply { sort?.let { addQueryParameter("sort", it) } }
			.build()
			.toString()
	}

	private fun SortOrder.toInkSort(): String? = when (this) {
		SortOrder.POPULARITY,
		SortOrder.POPULARITY_ASC,
		-> "views"

		SortOrder.UPDATED,
		SortOrder.UPDATED_ASC,
		SortOrder.NEWEST,
		SortOrder.NEWEST_ASC,
		-> "date"

		SortOrder.ALPHABETICAL,
		SortOrder.ALPHABETICAL_DESC,
		-> "title"

		SortOrder.RATING,
		SortOrder.RATING_ASC,
		-> "rating"

		SortOrder.RELEVANCE -> null
		else -> null
	}

	private fun parseInkCards(doc: Document): List<Manga> {
		return doc.select("a.ink-card[href*=/obras/]").mapNotNull { a ->
			val href = a.attrAsRelativeUrl("href")
			if (!href.startsWith("/obras/") || href.count { it == '/' } < 2) {
				return@mapNotNull null
			}
			val title = a.selectFirst(".ink-card-title")?.text()?.trim()
				?: a.attr("title").trim()
			if (title.isEmpty()) return@mapNotNull null
			Manga(
				id = generateUid(href),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				title = title,
				coverUrl = a.selectFirst("img")?.src(),
				altTitles = emptySet(),
				rating = RATING_UNKNOWN,
				tags = emptySet(),
				description = null,
				state = null,
				authors = emptySet(),
				contentRating = ContentRating.ADULT,
				source = source,
			)
		}.distinctBy { it.url }
	}
}
