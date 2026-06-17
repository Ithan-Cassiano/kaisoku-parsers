package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("ARCTICSCAN", "ArcticScan", "pt")
internal class ArcticScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ARCTICSCAN, "arcticscan.top") {
	override val datePattern: String = "yyyy-MM-dd"
}
