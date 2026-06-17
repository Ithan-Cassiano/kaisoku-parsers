package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("RUYAMANGA", "RuyaManga", "tr")
internal class RuyaManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.RUYAMANGA, "ruyamanga.net", 18) {
	override val tagPrefix = "manga-kategori/"
	override val datePattern = "dd/MM/yyyy"
}
