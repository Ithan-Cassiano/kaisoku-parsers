package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("SWEETSCAN", "SweetScan", "pt")
internal class SweetScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SWEETSCAN, "sweetscan.net") {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}
