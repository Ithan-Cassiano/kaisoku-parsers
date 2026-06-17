package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("BANANA_MANGA", "BananaManga", "en")
internal class BananaManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BANANA_MANGA, "bananamanga.net", 20)
