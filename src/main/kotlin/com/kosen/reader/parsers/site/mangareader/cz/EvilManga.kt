package com.kosen.reader.parsers.site.mangareader.cz

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("EVILMANGA", "EvilManga", "cs", ContentType.HENTAI)
internal class EvilManga(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.EVILMANGA, "evil-manga.eu", pageSize = 20, searchPageSize = 10) {
	override val datePattern = "d MMMM, yyyy"
}
