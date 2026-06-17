package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("YAOIX3", "3XYaoi", "pt", ContentType.HENTAI)
internal class YaoiX3(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YAOIX3, "3xyaoi.com") {
	override val datePattern = "dd/MM/yyyy"
	override val listUrl = "bl/"
	override val tagPrefix = "genero/"
}
