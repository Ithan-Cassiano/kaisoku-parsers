package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("RAINBOWFAIRYSCAN", "RainbowFairyScan", "pt")
internal class RainbowFairyScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.RAINBOWFAIRYSCAN, "rainbowfairyscan.com", 10) {
	override val datePattern: String = "dd/MM/yyyy"
}
