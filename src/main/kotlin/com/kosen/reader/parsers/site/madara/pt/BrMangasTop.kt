package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("BRMANGASTOP", "BrMangasTop", "pt")
internal class BrMangasTop(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BRMANGASTOP, "brmangas.top", 10) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}
