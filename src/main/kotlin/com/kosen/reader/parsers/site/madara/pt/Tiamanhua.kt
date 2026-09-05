package com.kosen.reader.parsers.site.madara.pt

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.exception.ParseException
import com.kosen.reader.parsers.model.ContentRating
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaListFilter
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.RATING_UNKNOWN
import com.kosen.reader.parsers.model.SortOrder
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.oneOrThrowIfMany
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.requireSrc
import com.kosen.reader.parsers.util.selectOrThrow
import com.kosen.reader.parsers.util.src
import com.kosen.reader.parsers.util.textOrNull
import com.kosen.reader.parsers.util.toAbsoluteUrl
import com.kosen.reader.parsers.util.toRelativeUrl
import com.kosen.reader.parsers.util.urlEncoded

@MangaSourceParser("TIAMANHUA", "TiaManhwa", "pt", ContentType.HENTAI)
internal class Tiamanhua(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TIAMANHUA, "tiamanhwa.com", pageSize = 18) {

	override val listUrl = ""
	override val tagPrefix = "tag-manhwa/"
	override val datePattern = "dd/MM/yyyy"
	override val withoutAjax = true
	override val selectChapter = "li.wp-manga-chapter, li.chapter-item, div.chapter, div.wp-manga-chapter"
	override val selectBodyPage = "div.main-col-inner div.reading-content, div.reading-content"

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
		keys.add(ConfigKey.DisableUpdateChecking(defaultValue = true))
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter) =
		if (needsSearchEndpoint(filter)) {
			parseMangaList(loadDocument(buildSearchUrl(page, order, filter)))
		} else {
			parseMangaList(loadDocument(buildArchiveUrl(page, filter)))
		}

	override suspend fun getDetails(manga: Manga): Manga {
		val fullUrl = manga.url.toAbsoluteUrl(domain)
		val doc = loadDocument(fullUrl)
		val chapters = if (doc.select(selectTestAsync).isEmpty()) {
			runCatching { loadChapters(manga.url, doc) }.getOrDefault(emptyList())
				.ifEmpty { getChapters(manga, doc) }
		} else {
			getChapters(manga, doc)
		}
		return manga.copy(
			title = doc.selectFirst("h1")?.textOrNull() ?: manga.title,
			description = doc.select(selectDesc).html().takeIf { it.isNotBlank() } ?: manga.description,
			chapters = chapters,
			contentRating = ContentRating.ADULT,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = loadDocument(fullUrl)
		val root = doc.body().selectFirst(selectBodyPage)
			?: throw ParseException("Não foi possível carregar as páginas", fullUrl)
		return root.select(selectPage).flatMap { div ->
			div.selectOrThrow("img").map { img ->
				val url = img.requireSrc().toRelativeUrl(domain)
				MangaPage(
					id = generateUid(url),
					url = url,
					preview = null,
					source = source,
				)
			}
		}
	}

	override fun parseMangaList(doc: Document): List<Manga> {
		val parsed = super.parseMangaList(doc)
		if (parsed.isNotEmpty()) {
			return parsed
		}
		val slider = doc.select("#manga-slider-2 .slider__item, .slider__item, #loop-content .page-item-detail")
		if (slider.isEmpty()) {
			return emptyList()
		}
		return slider.mapNotNull { element ->
			val anchor = element.selectFirst("a[href*='/manhwa/'], a[href]") ?: return@mapNotNull null
			val href = anchor.attr("href").toRelativeUrl(domain)
			if (href.isBlank() || href == "/") {
				return@mapNotNull null
			}
			val title = element.selectFirst("h3 a, h4 a, .post-title a")?.textOrNull()
				?: anchor.attr("title").takeIf { it.isNotBlank() }
				?: href.substringAfter("/manhwa/").substringBefore('/').replace('-', ' ')
			Manga(
				id = generateUid(href),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				coverUrl = element.selectFirst("img")?.src(),
				title = title,
				altTitles = emptySet(),
				authors = emptySet(),
				tags = emptySet(),
				rating = RATING_UNKNOWN,
				state = null,
				source = source,
				contentRating = ContentRating.ADULT,
			)
		}.distinctBy { it.url }
	}

	private fun needsSearchEndpoint(filter: MangaListFilter): Boolean =
		!filter.query.isNullOrEmpty() ||
			filter.states.isNotEmpty() ||
			filter.contentRating != null ||
			filter.year != 0 ||
			!filter.author.isNullOrEmpty()

	private fun buildSearchUrl(page: Int, order: SortOrder, filter: MangaListFilter): String {
		val pages = page + 1
		return buildString {
			append("https://")
			append(domain)
			if (pages > 1) {
				append("/page/")
				append(pages)
			}
			append("/?s=")
			filter.query?.let { append(it.urlEncoded()) }
			append("&post_type=wp-manga")
			append("&m_orderby=")
			append(
				when (order) {
					SortOrder.POPULARITY -> "views"
					SortOrder.NEWEST -> "new-manga"
					SortOrder.ALPHABETICAL -> "alphabet"
					SortOrder.RATING -> "rating"
					else -> "latest"
				},
			)
		}
	}

	private fun buildArchiveUrl(page: Int, filter: MangaListFilter): String {
		val pageNum = page + 1
		return buildString {
			append("https://")
			append(domain)
			append('/')
			if (filter.tags.isNotEmpty()) {
				append(tagPrefix)
				append(filter.tags.oneOrThrowIfMany()?.key)
				append('/')
			}
			if (pageNum > 1) {
				append("page/")
				append(pageNum)
				append('/')
			}
		}
	}

	private suspend fun loadDocument(url: String): Document {
		val httpDoc = runCatching { webClient.httpGet(url).parseHtml() }.getOrNull()
		if (httpDoc != null && !isChallengePage(httpDoc)) {
			return httpDoc
		}
		return captureDocument(url)
	}

	private suspend fun captureDocument(url: String): Document {
		val script = """
			(() => {
				const html = document.documentElement ? document.documentElement.outerHTML : '';
				const blocked = html.indexOf('lsrecaptcha') >= 0 ||
					html.indexOf('Bot Verification') >= 0 ||
					html.indexOf('Verifying that you are not a robot') >= 0 ||
					html.indexOf('cf-challenge') >= 0 ||
					html.indexOf('Just a moment') >= 0;
				if (blocked) return null;
				const ready = document.querySelector('div.page-item-detail, .slider__item, div.summary_content, .post-title, div.reading-content, div.page-break, li.wp-manga-chapter');
				if (!ready) return null;
				window.stop();
				return document.documentElement.outerHTML;
			})();
		""".trimIndent()
		val rawHtml = context.evaluateJs(url, script, 30000L)
			?: throw ParseException("TiaManhwa pediu verificação. Abra a fonte no navegador interno e tente de novo.", url)
		return Jsoup.parse(unwrapJsString(rawHtml), url)
	}

	private fun isChallengePage(doc: Document): Boolean {
		val title = doc.title()
		val html = doc.html()
		return title.contains("Bot Verification", ignoreCase = true) ||
			html.contains("lsrecaptcha") ||
			html.contains("Verifying that you are not a robot") ||
			html.contains("cf-challenge") ||
			html.contains("Just a moment")
	}

	private fun unwrapJsString(raw: String): String {
		if (!(raw.startsWith("\"") && raw.endsWith("\""))) {
			return raw
		}
		return raw.substring(1, raw.length - 1)
			.replace("\\\"", "\"")
			.replace("\\n", "\n")
			.replace("\\r", "\r")
			.replace("\\t", "\t")
			.replace(Regex("""\\u([0-9A-Fa-f]{4})""")) { match ->
				match.groupValues[1].toInt(16).toChar().toString()
			}
	}
}
