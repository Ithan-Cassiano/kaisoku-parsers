package com.kosen.reader.parsers.site.madara.vi

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.*

@MangaSourceParser("QUAANHDAOCUTEO", "Quả Anh Đào Cuteo", "vi", ContentType.HENTAI)
internal class Quaanhdaocuteo(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.QUAANHDAOCUTEO, "qadcuteo.cc") {
	override val datePattern = "dd/MM/yyyy"
	override val selectPage = "p img"

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(fullUrl).parseHtml()
		val root = doc.body().selectFirstOrThrow(selectBodyPage)
		return root.select(selectPage).map { img ->
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
