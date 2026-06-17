package com.kosen.reader.parsers.site.pizzareader.it

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("TUTTOANIMEMANGA", "TuttoAnimeManga", "it")
internal class TuttoAnimeManga(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.TUTTOANIMEMANGA, "tuttoanimemanga.net") {
	override val completedFilter = "completato"
}
