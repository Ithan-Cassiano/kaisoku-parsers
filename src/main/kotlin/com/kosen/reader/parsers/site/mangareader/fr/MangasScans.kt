package com.kosen.reader.parsers.site.mangareader.fr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANGASSCANS", "MangasScans", "fr")
internal class MangasScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGASSCANS, "mangas-scans.com", pageSize = 30, searchPageSize = 10)
