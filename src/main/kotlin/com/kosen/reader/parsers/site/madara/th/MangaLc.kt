package com.kosen.reader.parsers.site.madara.th

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGALC", "MangaLc", "th")
internal class MangaLc(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGALC, "manga-lc.net", 24) {
	override val datePattern: String = "d MMMM yyyy"
	override val selectPage = "img"
}
