package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("BARMANGA", "BarManga", "es")
internal class BarManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BARMANGA, "barmanga.com") {
	override val datePattern = "MM/dd/yyyy"
}
