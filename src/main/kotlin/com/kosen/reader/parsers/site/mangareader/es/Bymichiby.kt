package com.kosen.reader.parsers.site.mangareader.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("BYMICHIBY", "Bymichiby", "es", ContentType.HENTAI)
internal class Bymichiby(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.BYMICHIBY, "bymichiby.com", pageSize = 20, searchPageSize = 10)
