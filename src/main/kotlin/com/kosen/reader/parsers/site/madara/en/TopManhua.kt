package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TOPMANHUA", "ManhuaTop", "en")
internal class TopManhua(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TOPMANHUA, "manhuatop.org") {
	override val tagPrefix = "manhua-genre/"
	override val listUrl = "manhua/"
	override val datePattern = "MM/dd/yyyy"
}
