package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LERHENTAI", "LerHentai", "pt", ContentType.HENTAI)
internal class LerHentai(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LERHENTAI, "lerhentai.com", 20) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}
