package com.kosen.reader.parsers.site.cupfox.fr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.cupfox.CupFoxParser

@MangaSourceParser("SEINEMANGA", "SeineManga", "fr")
internal class SeineManga(context: MangaLoaderContext) :
	CupFoxParser(context, MangaParserSource.SEINEMANGA, "www.seinemanga.com")
