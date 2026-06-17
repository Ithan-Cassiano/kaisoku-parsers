package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("NEROXUS", "Neroxus", "pt")
internal class Neroxus(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NEROXUS, "neroxus.com.br", 10) {
	override val datePattern = "MMM d, yyyy"
}
