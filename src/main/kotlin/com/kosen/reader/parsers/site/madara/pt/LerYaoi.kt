package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LERYAOI", "LerYaoi", "pt", ContentType.HENTAI)
internal class LerYaoi(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LERYAOI, "leryaoi.com", 10) {
	override val datePattern = "dd/MM/yyyy"
	override val listUrl = "bl/"
	override val tagPrefix = "genero/"
}
