package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("SAMURAISCAN", "SamuraiScan", "es")
internal class SamuraiScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SAMURAISCAN, "samuraiscan.com", 10) {
	override val listUrl = "read/"
}
