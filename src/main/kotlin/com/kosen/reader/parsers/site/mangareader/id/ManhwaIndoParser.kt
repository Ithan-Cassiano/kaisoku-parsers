package com.kosen.reader.parsers.site.mangareader.id

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.nodes.Element
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.util.*

@MangaSourceParser("MANHWAINDO", "ManhwaIndo", "id")
internal class ManhwaIndoParser(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANHWAINDO, "manhwaindo.one", pageSize = 30, searchPageSize = 10) {
	override val datePattern = "MMM d, yyyy"
	override val listUrl = "/series"

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterUrl = chapter.url.toAbsoluteUrl(domain)
		val docs = webClient.httpGet(chapterUrl).parseHtml()
		return coroutineScope {
			docs.select(selectPage).map { img ->
				async { fetchPage(img) }
			}.awaitAll().filterNotNull()
		}
	}

	private suspend fun fetchPage(img: Element): MangaPage? = runCatchingCancellable {
		val url = img.requireSrc().toAbsoluteUrl(domain)
		webClient.httpHead(url).use { response ->
			if (response.mimeType?.startsWith("image/") == true) {
				MangaPage(
					id = generateUid(url),
					url = url,
					preview = null,
					source = source,
				)
			} else {
				null
			}
		}
	}.getOrNull()
}
