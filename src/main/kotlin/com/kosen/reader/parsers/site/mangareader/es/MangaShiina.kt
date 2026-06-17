package com.kosen.reader.parsers.site.mangareader.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANGASHIINA", "MangaMukai.com", "es")
internal class MangaShiina(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGASHIINA, "mangamukai.com", pageSize = 20, searchPageSize = 10)
