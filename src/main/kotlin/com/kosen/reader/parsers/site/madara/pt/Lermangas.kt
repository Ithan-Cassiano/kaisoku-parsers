package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LERMANGAS", "Lermangas", "pt")
internal class Lermangas(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LERMANGAS, "lermangas.me", pageSize = 20) {
	override val datePattern = "dd 'de' MMMMM 'de' yyyy"
}
