package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("LOLICONMOBI", "LoliconMobi", "en", ContentType.HENTAI)
internal class LoliconMobi(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LOLICONMOBI, "lolicon.mobi") {
	override val postReq = true
	override val tagPrefix = "lolicon-genre/"
}
