package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAWOW", "MangaWow", "tr")
internal class MangaWow(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAWOW, "mangawow.org", 18) {
	override val datePattern = "d MMMM yyyy"
}
