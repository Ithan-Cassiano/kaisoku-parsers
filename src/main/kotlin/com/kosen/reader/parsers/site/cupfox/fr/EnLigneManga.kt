package com.kosen.reader.parsers.site.cupfox.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.cupfox.CupFoxParser

@Broken
@MangaSourceParser("ENLIGNEMANGA", "EnLigneManga", "fr")
internal class EnLigneManga(context: MangaLoaderContext) :
	CupFoxParser(context, MangaParserSource.ENLIGNEMANGA, "www.enlignemanga.com")
