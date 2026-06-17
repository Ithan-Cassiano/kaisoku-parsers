package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TERRITORIOLEAL", "TerritorioLeal", "es")
internal class TerritorioLeal(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TERRITORIOLEAL, "territorioleal.com") {
	override val datePattern = "d 'de' MMMM 'de' yyyy"
}
