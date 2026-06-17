package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGALEVELING", "MangaLeveling", "en")
internal class MangaLeveling(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGALEVELING, "mangaleveling.com", 30) {
	override val postReq = true
	override val tagPrefix = "comics-genre/"
	override val listUrl = "comics/"
	override val datePattern = "MM/dd/yyyy"
}
