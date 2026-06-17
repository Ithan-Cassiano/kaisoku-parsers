package com.kosen.reader.parsers.site.foolslide.es

import kotlinx.coroutines.coroutineScope
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.foolslide.FoolSlideParser
import com.kosen.reader.parsers.util.*

@MangaSourceParser("SEINAGIADULTO", "Seinagi Adulto", "es", ContentType.HENTAI)
internal class SeinagiAdulto(context: MangaLoaderContext) :
	FoolSlideParser(context, MangaParserSource.SEINAGIADULTO, "adulto.seinagi.org.es") {

	override suspend fun getDetails(manga: Manga): Manga = coroutineScope {
		val fullUrl = manga.url.toAbsoluteUrl(domain)
		val testAdultPage = webClient.httpGet(fullUrl).parseHtml()
		val doc = if (testAdultPage.selectFirst("div.info form") != null) {
			webClient.httpPost(fullUrl, "adult=true").parseHtml()
		} else {
			testAdultPage
		}
		val chapters = getChapters(doc)
		val desc = if (doc.selectFirstOrThrow(selectInfo).html().contains("Descripción")) {
			doc.selectFirst(selectInfo)?.text()?.substringAfter("Descripción: ")?.substringBefore("Lecturas")
		} else {
			doc.selectFirst(selectInfo)?.text()
		}
		val author = if (doc.selectFirst(selectInfo)?.html()?.contains("Author") == true) {
			doc.selectFirst(selectInfo)?.text()?.substringAfter("Author: ")?.substringBefore("Art")
		} else {
			null
		}
		manga.copy(
			coverUrl = doc.selectFirst(".thumbnail img")?.src(),// for manga result on search
			description = desc?.nullIfEmpty(),
			authors = author?.nullIfEmpty()?.let { setOf(it) } ?: emptySet(),
			chapters = chapters,
		)
	}
}
