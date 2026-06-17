package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("INFRAFANDUB", "InfraFandub", "es")
internal class Infrafandub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.INFRAFANDUB, "infrafandub.com", 10) {
	override val datePattern = "dd/MM/yyyy"
}
