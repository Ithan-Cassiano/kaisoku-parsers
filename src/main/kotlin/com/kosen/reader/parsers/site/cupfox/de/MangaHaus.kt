package com.kosen.reader.parsers.site.cupfox.de

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.cupfox.CupFoxParser

@MangaSourceParser("MANGAHAUS", "MangaHaus", "de")
internal class MangaHaus(context: MangaLoaderContext) :
	CupFoxParser(context, MangaParserSource.MANGAHAUS, "www.mangahaus.com")
