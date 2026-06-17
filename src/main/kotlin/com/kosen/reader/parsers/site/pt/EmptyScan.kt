package com.kosen.reader.parsers.site.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource

@MangaSourceParser("EMPTYSCAN", "EmptyScan", "pt")
internal class EmptyScan(context: MangaLoaderContext) :
	SkkyScanParser(context, MangaParserSource.EMPTYSCAN, "emptyscan.site")
