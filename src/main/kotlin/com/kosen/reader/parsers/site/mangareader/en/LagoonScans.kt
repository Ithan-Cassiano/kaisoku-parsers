package com.kosen.reader.parsers.site.mangareader.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("LAGOONSCANS", "Lagoon Scans", "en")
internal class LagoonScans(context: MangaLoaderContext) :
    MangaReaderParser(context, MangaParserSource.LAGOONSCANS, "lagoonscans.com", pageSize = 20, searchPageSize = 10)
