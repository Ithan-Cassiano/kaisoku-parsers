package com.kosen.reader.parsers.site.pizzareader.it

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.pizzareader.PizzaReaderParser

@MangaSourceParser("GTOTHEGREATSITE", "GtoTheGreatSite", "it")
internal class GtoTheGreatSite(context: MangaLoaderContext) :
	PizzaReaderParser(context, MangaParserSource.GTOTHEGREATSITE, "reader.gtothegreatsite.net")
