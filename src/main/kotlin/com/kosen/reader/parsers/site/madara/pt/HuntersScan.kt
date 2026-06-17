package com.kosen.reader.parsers.site.madara.pt

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.Protocol
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentRating
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaState
import com.kosen.reader.parsers.model.RATING_UNKNOWN
import com.kosen.reader.parsers.network.OkHttpWebClient
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.attrAsRelativeUrl
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.mapChapters
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.removeSuffix
import com.kosen.reader.parsers.util.selectFirstOrThrow
import com.kosen.reader.parsers.util.textOrNull
import com.kosen.reader.parsers.util.toAbsoluteUrl
import java.text.SimpleDateFormat

@Broken
@MangaSourceParser("HUNTERSSCAN", "HuntersScan", "pt")
internal class HuntersScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HUNTERSSCAN, "readhunters.xyz", pageSize = 50) {

	override val datePattern = "dd/MM/yyyy"
	override val tagPrefix = "series-genre/"
	override val listUrl = "series/"

	// Custom HTTP/1.1 client to fix 421 SNI mismatch error
	private val http11Client = context.httpClient.newBuilder()
		.protocols(listOf(Protocol.HTTP_1_1))
		.build()

	// Custom webClient using HTTP/1.1
	private val http11WebClient = OkHttpWebClient(
		httpClient = http11Client,
		mangaSource = source
	)

	override suspend fun getChapters(manga: Manga, doc: Document): List<MangaChapter> {
		return fetchAllChapters(manga)
	}

	override suspend fun loadChapters(mangaUrl: String, document: Document): List<MangaChapter> {
		return fetchAllChapters(
			Manga(
				id = generateUid(mangaUrl),
				url = mangaUrl,
				publicUrl = mangaUrl.toAbsoluteUrl(domain),
				title = "",
				altTitles = emptySet(),
				authors = emptySet(),
				tags = emptySet(),
				rating = RATING_UNKNOWN,
				state = MangaState.ONGOING,
				coverUrl = null,
				contentRating = ContentRating.SAFE,
				source = source,
			),
		)
	}

	private suspend fun fetchAllChapters(manga: Manga): List<MangaChapter> = coroutineScope {
		val baseUrl = "${manga.url.toAbsoluteUrl(domain).removeSuffix('/')}/ajax/chapters/?t="
		val dateFormat = SimpleDateFormat(datePattern, sourceLocale)

		// Fetch first page using HTTP/1.1 client
		val firstPageDoc = http11WebClient.httpPost(baseUrl + "1", emptyMap()).parseHtml()
		val totalPages = extractTotalPages(firstPageDoc)
		val firstPageChapters = firstPageDoc.select(selectChapter).map { parseChapterElement(it, dateFormat) }

		if (totalPages <= 1) {
			return@coroutineScope firstPageChapters.mapChapters(reversed = true) { index, chapter ->
				chapter.copy(number = (firstPageChapters.size - index).toFloat())
			}
		}

		// Fetch remaining pages concurrently using HTTP/1.1 client
		val remainingPagesChapters = (2..totalPages).chunked(10).flatMap { batch ->
			batch.map { page ->
				async {
					try {
						val doc = http11WebClient.httpPost(baseUrl + page, emptyMap()).parseHtml()
						doc.select(selectChapter).map {
							parseChapterElement(it, dateFormat)
						}
					} catch (e: Exception) {
						emptyList()
					}
				}
			}.awaitAll().flatten()
		}

		val allChapters = firstPageChapters + remainingPagesChapters
		allChapters.mapChapters(reversed = true) { index, chapter ->
			chapter.copy(number = (allChapters.size - index).toFloat())
		}

	}

	private fun extractTotalPages(doc: Document): Int {
		val pagination = doc.selectFirst(".pagination") ?: return 1

		return pagination.select("a[data-page]").mapNotNull { it.attr("data-page").toIntOrNull() }.maxOrNull() ?: 1
	}

	private fun parseChapterElement(li: Element, dateFormat: SimpleDateFormat): MangaChapter {
		val a = li.selectFirstOrThrow("a")
		val href = a.attrAsRelativeUrl("href")

		return MangaChapter(
			id = generateUid(href),
			title = a.ownText().ifEmpty { a.text() },
			number = 0f, // Will be set later
			volume = 0,
			url = href + stylePage,
			uploadDate = parseChapterDate(dateFormat, li.selectFirst(selectDate)?.textOrNull()),
			source = source,
			scanlator = null,
			branch = null,
		)
	}
}

