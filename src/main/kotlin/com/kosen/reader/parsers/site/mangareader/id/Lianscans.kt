package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("LIANSCANS", "LianScans", "id", ContentType.HENTAI)
internal class Lianscans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.LIANSCANS, "www.lianscans.com", pageSize = 10, searchPageSize = 10) {
	override val datePattern = "MMM d, yyyy"
}
