package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LICHMANGAS", "LichMangas", "pt")
internal class LichMangas(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LICHMANGAS, "lichmangas.com", 10) {
	override val datePattern = "dd/MM/yyyy"
}
