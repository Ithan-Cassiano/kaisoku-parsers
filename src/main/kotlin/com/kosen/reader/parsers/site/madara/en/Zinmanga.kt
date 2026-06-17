package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("ZINMANGA", "ZinManga.net", "en")
internal class Zinmanga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ZINMANGA, "zinmanga.net") {
	override val datePattern = "MM/dd/yyyy"
	override val withoutAjax = true

    override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
        .set("Referer", "https://www.zinmanga.net/")
        .build()
}
