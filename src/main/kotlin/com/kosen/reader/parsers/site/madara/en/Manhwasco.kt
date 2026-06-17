package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHWASCO", "ManhwaSco", "en")
internal class Manhwasco(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWASCO, "manhwasco.net") {
	override val selectGenre = "div.mg_genres a"
}
