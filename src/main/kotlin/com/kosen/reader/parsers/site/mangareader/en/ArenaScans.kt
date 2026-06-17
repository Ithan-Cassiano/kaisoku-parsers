package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("ARENASCANS", "Arenascans", "en")
internal class ArenaScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.ARENASCANS, "arenascan.com", pageSize = 20, searchPageSize = 10)
