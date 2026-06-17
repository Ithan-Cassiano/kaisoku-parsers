package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAROCK", "MangaRock", "en")
internal class MangaRock(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAROCK, "mangarockteam.com") {
	override val datePattern = "MMMM dd, yyyy"
}
