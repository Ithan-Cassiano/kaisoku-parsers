package org.koitharu.kotatsu.parsers.site.en

import androidx.collection.arraySetOf
import okhttp3.Interceptor
import okhttp3.Response
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("DEMONICSCANS", "Manga Demon", "en")
internal class DemonicScans(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.DEMONICSCANS, 25) {

	override val configKeyDomain = ConfigKey.Domain("demonicscans.org")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.NEWEST,
		SortOrder.POPULARITY,
		SortOrder.ALPHABETICAL,
		SortOrder.ALPHABETICAL_DESC,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
			isMultipleTagsSupported = true,
		)

	override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
		.add("Referer", "https://$domain/")
		.build()

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val builder = request.newBuilder()
		if (request.header("Referer").isNullOrEmpty()) {
			builder.header("Referer", "https://$domain/")
		}
		return chain.proceed(builder.build())
	}

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchTags(),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val hasQuery = !filter.query.isNullOrEmpty()
		val useLatest = !hasQuery && filter.tags.isEmpty() && order in LATEST_ORDERS
		val url = when {
			hasQuery -> "https://$domain/search.php?manga=${filter.query.urlEncoded()}"
			useLatest -> "https://$domain/lastupdates.php?list=$page"
			order == SortOrder.ALPHABETICAL -> buildAdvancedUrl(page, filter, "NAME ASC")
			order == SortOrder.ALPHABETICAL_DESC -> buildAdvancedUrl(page, filter, "NAME DESC")
			else -> buildAdvancedUrl(page, filter, "VIEWS DESC")
		}

		val doc = webClient.httpGet(url).parseHtml()
		val selector = when {
			hasQuery -> "a[href*=/manga/]"
			useLatest -> "div#updates-container > div.updates-element"
			else -> "div#advanced-content > div.advanced-element"
		}

		return doc.select(selector).mapNotNull { element ->
			when {
				useLatest -> parseNewestManga(element)
				else -> parseNormalManga(element, hasQuery)
			}
		}.filter { it.title.isNotBlank() && isValidMangaUrl(it.url) }
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url).parseHtml()
		val info = doc.selectFirst("div#manga-info-container")
		val title = info?.selectFirst("h1.big-fat-titles")?.textOrNull().orEmpty()
		val thumbnail = info?.selectFirst("div#manga-page img")?.attrAsAbsoluteUrlOrNull("src")
		val genre = info?.select("div.genres-list > li")?.joinToString { it.text() }.orEmpty()
		val description = info?.selectFirst("div#manga-info-rightColumn div.white-font")?.textOrNull()
		val author = info?.selectFirst("div#manga-info-stats > div:has(> li:first-child:contains(Author)) > li:nth-child(2)")
			?.textOrNull()
		val statusText = info?.selectFirst("div#manga-info-stats > div:has(> li:first-child:contains(Status)) > li:nth-child(2)")
			?.textOrNull()
		val state = when (statusText?.trim()) {
			"Ongoing" -> MangaState.ONGOING
			"Completed" -> MangaState.FINISHED
			else -> null
		}

		val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
		val chapters = doc.select("div#chapters-list a.chplinks").mapChapters(reversed = true) { _, el ->
			val href = el.attr("href").toAbsoluteUrl(domain)
			val date = el.selectFirst("span")?.text()
			val chapterNumber = CHAPTER_NUMBER_REGEX.find(el.text())?.groupValues?.get(1)?.toFloatOrNull()
			MangaChapter(
				id = generateUid(href),
				title = el.ownText().trim(),
				number = chapterNumber ?: 0f,
				volume = 0,
				url = href,
				scanlator = null,
				uploadDate = dateFormat.parseSafe(date),
				branch = null,
				source = source,
			)
		}

		return manga.copy(
			title = title.ifBlank { manga.title },
			coverUrl = thumbnail ?: manga.coverUrl,
			tags = genre.split(", ").filter { it.isNotBlank() }.mapToSet {
				MangaTag(title = it.lowercase().replace(" ", "-").toTitleCase(sourceLocale), key = it, source)
			},
			description = description,
			state = state,
			authors = if (author.isNullOrBlank()) emptySet() else setOf(author.trim()),
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url).parseHtml()
		return doc.select("img.imgholder[src]").mapNotNull { img ->
			val url = img.attr("abs:src").ifEmpty {
				img.src()?.toAbsoluteUrl(domain) ?: return@mapNotNull null
			}
			if (!isChapterImage(url)) {
				return@mapNotNull null
			}
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}

	private fun buildAdvancedUrl(page: Int, filter: MangaListFilter, orderBy: String) = buildString {
		append("https://$domain/advanced.php")
		append("?list=$page")
		append("&status=all")
		append("&orderby=")
		append(orderBy.urlEncoded())
		filter.tags.forEach { tag ->
			append("&genre[]=")
			append(tag.key.urlEncoded())
		}
	}

	private fun parseNewestManga(element: Element): Manga? {
		val anchor = element.selectFirst("div.updates-element-info h2 a") ?: return null
		val href = anchor.attr("href").toAbsoluteUrl(domain)
		return Manga(
			id = generateUid(href),
			title = anchor.text().trim(),
			altTitles = emptySet(),
			url = href,
			publicUrl = href,
			rating = RATING_UNKNOWN,
			contentRating = null,
			coverUrl = element.selectFirst("div.thumb img")?.attrAsAbsoluteUrlOrNull("src"),
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			source = source,
		)
	}

	private fun parseNormalManga(element: Element, hasQuery: Boolean): Manga? {
		val anchor = if (hasQuery) element else element.selectFirst("a") ?: return null
		val href = anchor.attr("href").toAbsoluteUrl(domain)
		if (!isValidMangaUrl(href)) {
			return null
		}
		val title = when {
			hasQuery -> element.selectFirst("div.seach-right > div")?.text()?.trim()
			else -> anchor.attr("title").trim().ifEmpty {
				anchor.selectFirst("h1")?.ownText()?.trim()
			}
		}.orEmpty()
		return Manga(
			id = generateUid(href),
			title = title,
			altTitles = emptySet(),
			url = href,
			publicUrl = href,
			rating = RATING_UNKNOWN,
			contentRating = null,
			coverUrl = anchor.selectFirst("img")?.attrAsAbsoluteUrlOrNull("src"),
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			source = source,
		)
	}

	private fun isValidMangaUrl(url: String): Boolean {
		return url.contains("/manga/") && !url.endsWith("/manga/") && !url.endsWith("/manga")
	}

	private fun isChapterImage(url: String): Boolean {
		if (url.contains("/img/free_ads") || url.endsWith("/img/free_ads.jpg")) {
			return false
		}
		return url.contains("demoniclibs.com") ||
			url.contains("librarydm.com") ||
			url.contains("mangareadon.org") ||
			url.contains("readermc.org")
	}

	private fun fetchTags() = arraySetOf(
		MangaTag("Action", "1", source),
		MangaTag("Adventure", "2", source),
		MangaTag("Comedy", "3", source),
		MangaTag("Cooking", "34", source),
		MangaTag("Doujinshi", "25", source),
		MangaTag("Drama", "4", source),
		MangaTag("Ecchi", "19", source),
		MangaTag("Fantasy", "5", source),
		MangaTag("Gender Bender", "30", source),
		MangaTag("Harem", "10", source),
		MangaTag("Historical", "28", source),
		MangaTag("Horror", "8", source),
		MangaTag("Isekai", "33", source),
		MangaTag("Josei", "31", source),
		MangaTag("Martial Arts", "6", source),
		MangaTag("Mature", "22", source),
		MangaTag("Mecha", "32", source),
		MangaTag("Mystery", "15", source),
		MangaTag("One Shot", "26", source),
		MangaTag("Psychological", "11", source),
		MangaTag("Romance", "12", source),
		MangaTag("School Life", "13", source),
		MangaTag("Sci-fi", "16", source),
		MangaTag("Seinen", "17", source),
		MangaTag("Shoujo", "14", source),
		MangaTag("Shoujo Ai", "23", source),
		MangaTag("Shounen", "7", source),
		MangaTag("Shounen Ai", "29", source),
		MangaTag("Slice of Life", "21", source),
		MangaTag("Smut", "27", source),
		MangaTag("Sports", "20", source),
		MangaTag("Supernatural", "9", source),
		MangaTag("Tragedy", "18", source),
		MangaTag("Webtoons", "24", source),
	)

	private companion object {
		val LATEST_ORDERS = setOf(SortOrder.UPDATED, SortOrder.NEWEST)
		val CHAPTER_NUMBER_REGEX = Regex("""Chapter\s+([\d.]+)""", RegexOption.IGNORE_CASE)
	}
}
