package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("PIRULITOROSA", "PirulitoRosa", "pt", ContentType.HENTAI)
internal class Pirulitorosa(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PIRULITOROSA, "pirulitorosa.site") {
	override val postReq = true
	override val datePattern: String = "dd/MM/yyyy"
}
