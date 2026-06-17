package com.kosen.reader.parsers.site.madara.ru

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAZAVR", "Mangazavr", "ru", ContentType.HENTAI)
internal class Mangazavr(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAZAVR, "mangazavr.ru") {
	override val listUrl = "/?s=&post_type=wp-manga"
	override val datePattern = "dd.MM.yyyy"
}
