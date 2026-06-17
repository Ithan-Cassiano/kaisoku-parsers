package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LIMITEDTIMEPOJECT", "LimitedTimePoject", "pt")
internal class LimitedTimePoject(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LIMITEDTIMEPOJECT, "limitedtimeproject.com", 10) {
	override val listUrl = "manhwa/"
	override val tagPrefix = "manhwa-genero/"
	override val datePattern = "dd/MM/yyyy"
}
