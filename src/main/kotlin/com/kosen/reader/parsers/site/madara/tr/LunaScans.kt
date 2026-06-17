package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LUNASCANS", "LunaScans", "tr", ContentType.HENTAI)
internal class LunaScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LUNASCANS, "tuhafscans.com") {
	override val postReq = true
	override val datePattern = "dd MMMM yyyy"
}
