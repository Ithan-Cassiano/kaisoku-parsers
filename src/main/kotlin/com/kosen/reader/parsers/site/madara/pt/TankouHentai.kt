package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TANKOUHENTAI", "TankouHentai", "pt", ContentType.HENTAI)
internal class TankouHentai(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TANKOUHENTAI, "tankouhentai.com", pageSize = 16) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}
