package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("ANISASCANS", "AnisaScans", "en")
internal class AnisaScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ANISASCANS, "anisascans.in", 36) {
	override val datePattern = "dd MMM, yyyy"
}
