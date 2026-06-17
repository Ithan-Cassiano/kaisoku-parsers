package com.kosen.reader.parsers.site.pizzareader.it

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("HASTATEAM_READER", "HastaTeamReader", "it")
internal class HastaTeamReader(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.HASTATEAM_READER, "reader.hastateam.com")
