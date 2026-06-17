package com.kosen.reader.parsers.site.madara.ru

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGASHI", "Manga-shi", "ru")
internal class MangaShi(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGASHI, "manga-shi.org") {
	override val datePattern = "dd.MM.yyyy"
    override val withoutAjax = true
}
