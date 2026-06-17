package com.kosen.reader.parsers.site.mangareader.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("DTUPSCAN", "DtupScan", "es")
internal class DtupScan(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.DTUPSCAN, "dtupscan.com", pageSize = 20, searchPageSize = 10)
