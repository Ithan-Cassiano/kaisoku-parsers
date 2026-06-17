package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MUGIWARASOFICIAL", "MugiwarasOficial", "pt")
internal class MugiwarasOficial(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MUGIWARASOFICIAL, "mugiwarasoficial.com") {
	override val datePattern: String = "dd/MM/yyyy"
}
