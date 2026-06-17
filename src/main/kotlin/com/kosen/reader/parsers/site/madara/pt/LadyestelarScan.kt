package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("LADYESTELARSCAN", "LadyEstelarScan", "pt")
internal class LadyestelarScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LADYESTELARSCAN, "ladyestelarscan.com.br", 10) {
	override val datePattern: String = "dd/MM/yyyy"
}
