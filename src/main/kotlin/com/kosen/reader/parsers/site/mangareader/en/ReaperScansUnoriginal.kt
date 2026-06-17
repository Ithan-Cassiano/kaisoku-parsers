package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken("Domain offline - could not resolve host")
@MangaSourceParser("REAPERSCANSUNORIGINAL", "ReaperScansUnoriginal", "en")
internal class ReaperScansUnoriginal(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.REAPERSCANSUNORIGINAL,
		"reaper-scans.com",
		pageSize = 30,
		searchPageSize = 42,
	)
