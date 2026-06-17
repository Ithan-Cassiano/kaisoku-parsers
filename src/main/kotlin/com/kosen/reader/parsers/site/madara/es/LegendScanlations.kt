package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LEGENDSCANLATIONS", "LegendScanlations", "es")
internal class LegendScanlations(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LEGENDSCANLATIONS, "escaneodeleyendas.com", 10) {
	override val datePattern = "dd/MM/yyyy"
}
