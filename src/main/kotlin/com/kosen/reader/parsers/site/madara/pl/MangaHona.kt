package com.kosen.reader.parsers.site.madara.pl

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAHONA", "MangaHona", "pl")
internal class MangaHona(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAHONA, "mangahona.pl") {
	override val datePattern = "yyyy-MM-dd"
}
