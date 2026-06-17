package com.kosen.reader.parsers.site.madara.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.model.ContentType

@MangaSourceParser("WEBTOONEMPIRE", "WebtoonEmpire", "ar", type = ContentType.HENTAI)
internal class WebtoonEmpire(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.WEBTOONEMPIRE, "webtoonempire-bl.com", pageSize = 10) {
	override val listUrl = "webtoon/"
	override val datePattern = "d MMMM، yyyy"
	override val withoutAjax = true
}
