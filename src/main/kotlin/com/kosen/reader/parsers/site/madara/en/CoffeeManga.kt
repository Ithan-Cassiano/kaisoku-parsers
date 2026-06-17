package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("COFFEE_MANGA", "CoffeeManga", "en")
internal class CoffeeManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.COFFEE_MANGA, "coffeemanga.io")
