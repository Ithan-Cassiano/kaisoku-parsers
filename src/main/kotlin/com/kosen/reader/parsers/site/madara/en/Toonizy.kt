package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TOONIZY", "Toonizy", "en", ContentType.HENTAI)
internal class Toonizy(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TOONIZY, "toonizy.com", 24) {
	override val datePattern = "MMM d, yy"
	override val listUrl = "webtoon/"
}
