package com.kosen.reader.parsers.site.scan.fr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.scan.ScanParser

@MangaSourceParser("SCANTRAD", "ScanTrad", "fr")
internal class ScanTrad(context: MangaLoaderContext) :
	ScanParser(context, MangaParserSource.SCANTRAD, "scan-trad.com")
