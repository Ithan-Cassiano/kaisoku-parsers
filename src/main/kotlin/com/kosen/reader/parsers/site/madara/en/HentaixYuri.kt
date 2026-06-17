package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("HENTAIXYURI", "HentaiXYuri", "en", ContentType.HENTAI)
internal class HentaixYuri(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HENTAIXYURI, "hentaixyuri.com", 16) {
	override val postReq = true
}
