package com.kosen.reader.parsers.site.pizzareader.fr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("HNISCANTRAD", "HniScantrad", "fr")
internal class HniScantrad(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.HNISCANTRAD, "hni-scantrad.net") {
	override val ongoingFilter = "en cours"
	override val completedFilter = "terminé"
}
