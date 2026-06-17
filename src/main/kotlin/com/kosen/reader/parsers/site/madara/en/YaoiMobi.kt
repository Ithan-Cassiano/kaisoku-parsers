package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("YAOIMOBI", "Yaoi.Mobi", "en", ContentType.HENTAI)
internal class YaoiMobi(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YAOIMOBI, "yaoi.mobi") {
	override val postReq = true
}
