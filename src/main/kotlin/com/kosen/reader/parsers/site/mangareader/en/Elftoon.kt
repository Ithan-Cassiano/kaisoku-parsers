package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("ELFTOON", "Elftoon", "en")
internal class Elftoon(context: MangaLoaderContext) :
    MangaReaderParser(context, MangaParserSource.ELFTOON, "elftoon.com", pageSize = 20, searchPageSize = 10)
