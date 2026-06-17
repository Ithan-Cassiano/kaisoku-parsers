package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LILYMANGA", "LilyManga", "en", ContentType.HENTAI)
internal class LilyManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LILYMANGA, "lilymanga.net") {
	override val tagPrefix = "ys-genre/"
	override val listUrl = "ys/"
	override val datePattern = "yyyy-MM-dd"
}
