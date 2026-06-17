package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("ARAZNOVEL", "ArazNovel", "tr")
internal class ArazNovel(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ARAZNOVEL, "araznovel.com", 10) {
	override val datePattern = "d MMMM yyyy"
	override val postReq = true
}
