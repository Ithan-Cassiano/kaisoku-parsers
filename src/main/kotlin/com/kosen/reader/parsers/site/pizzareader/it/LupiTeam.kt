package com.kosen.reader.parsers.site.pizzareader.it

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("LUPITEAM", "LupiTeam", "it")
internal class LupiTeam(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.LUPITEAM, "lupiteam.net")
