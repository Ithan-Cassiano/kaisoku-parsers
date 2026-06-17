package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("PASSAMAOSCAN", "PassamaoScan", "pt")
internal class PassamaoScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PASSAMAOSCAN, "passamaoscan.com") {
	override val datePattern: String = "dd/MM/yyyy"
}
