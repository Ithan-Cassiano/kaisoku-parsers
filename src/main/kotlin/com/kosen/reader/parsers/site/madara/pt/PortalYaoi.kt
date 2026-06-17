package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("PORTALYAOI", "PortalYaoi", "pt", ContentType.HENTAI)
internal class PortalYaoi(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PORTALYAOI, "portalyaoi.com", 10) {
	override val tagPrefix = "genero/"
	override val datePattern: String = "dd/MM"
}
