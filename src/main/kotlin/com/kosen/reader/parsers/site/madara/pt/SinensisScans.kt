package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken("Not dead, changed template")
@MangaSourceParser("SINENSISSCANS", "SinensisScans", "pt")
internal class SinensisScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SINENSISSCANS, "sinensis.leitorweb.com") {
	override val datePattern: String = "dd/MM/yyyy"
}
