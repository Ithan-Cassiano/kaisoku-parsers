package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

//This source requires an account.
@MangaSourceParser("OPIATOON", "OpiaToon", "tr")
internal class OpiaToon(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.OPIATOON, "opiatoon.art", 20) {
	override val datePattern = "d MMMM"
}
