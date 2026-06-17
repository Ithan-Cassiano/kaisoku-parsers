package com.kosen.reader.parsers.site.madara.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken ( "Need refactor")
@MangaSourceParser("BLUESOLO", "BlueSolo", "fr")
internal class BlueSolo(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BLUESOLO, "www1.bluesolo.org", 10) {
	override val datePattern = "d MMMM yyyy"
}
