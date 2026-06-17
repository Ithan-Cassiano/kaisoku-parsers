package com.kosen.reader.parsers.site.scan.it

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.scan.ScanParser

@MangaSourceParser("MANGAITALIA", "MangaItalia", "it")
internal class MangaItalia(context: MangaLoaderContext) :
	ScanParser(context, MangaParserSource.MANGAITALIA, "mangaita.io")
