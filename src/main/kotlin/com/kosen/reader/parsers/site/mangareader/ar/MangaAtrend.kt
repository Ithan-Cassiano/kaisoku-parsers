package com.kosen.reader.parsers.site.mangareader.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANGAATREND", "MangaAtrend", "ar")
internal class MangaAtrend(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGAATREND, "mangaatrend.net", pageSize = 30, searchPageSize = 10)
