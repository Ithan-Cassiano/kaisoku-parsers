package com.kosen.reader.parsers.site.mangareader.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("STELLARSABER", "StellarSaber", "ar")
internal class StellarSaber(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.STELLARSABER, "stellarsaber.pro", pageSize = 32, searchPageSize = 10)
