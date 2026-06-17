package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser


@MangaSourceParser("MUGIMANGA", "MugiManga", "tr")
internal class MugiManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MUGIMANGA, "mugimanga.com", 20) {
	override val datePattern = "dd/MM/yyyy"
}
