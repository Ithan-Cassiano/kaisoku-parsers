package com.kosen.reader.parsers.site.madara.ru

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAONELOVE", "MangaOneLove", "ru")
internal class MangaoneLove(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAONELOVE, "mangaonelove.site", 10) {
	override val datePattern = "dd.MM.yyyy"
	override val postReq = true
}
