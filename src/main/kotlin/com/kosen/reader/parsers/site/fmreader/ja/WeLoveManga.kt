package com.kosen.reader.parsers.site.fmreader.ja

import org.jsoup.nodes.Document
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.fmreader.FmreaderParser
import com.kosen.reader.parsers.util.*
import java.text.SimpleDateFormat

@MangaSourceParser("WELOVEMANGA", "WeLoveManga", "ja")
internal class WeLoveManga(context: MangaLoaderContext) :
	FmreaderParser(context, MangaParserSource.WELOVEMANGA, "welovemanga.one") {

	override suspend fun getChapters(doc: Document): List<MangaChapter> {
		val mid = doc.selectFirstOrThrow("div.cmt input").attr("value")
		val docLoad =
			webClient.httpGet("https://$domain/app/manga/controllers/cont.Listchapter.php?mid=$mid").parseHtml()
		val dateFormat = SimpleDateFormat(datePattern, sourceLocale)
		return docLoad.body().select(selectChapter).mapChapters(reversed = true) { i, a ->
			val href = a.selectFirstOrThrow("a").attrAsRelativeUrl("href")
			val dateText = a.selectFirst(selectDate)?.text()
			MangaChapter(
				id = generateUid(href),
				title = a.selectFirstOrThrow("a").text(),
				number = i + 1f,
				volume = 0,
				url = href,
				uploadDate = parseChapterDate(
					dateFormat,
					dateText,
				),
				source = source,
				scanlator = null,
				branch = null,
			)
		}
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(fullUrl).parseHtml()
		val cid = doc.selectFirstOrThrow("#chapter").attr("value")
		val docLoad = webClient.httpGet("https://$domain/app/manga/controllers/cont.listImg.php?cid=$cid").parseHtml()
		return docLoad.select("img").map { img ->
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
