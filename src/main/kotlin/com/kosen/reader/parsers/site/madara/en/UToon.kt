package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("UTOON", "UToon", "en")
internal class UToon(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.UTOON, "utoon.net") {
	override val datePattern = "dd MMM"
}
