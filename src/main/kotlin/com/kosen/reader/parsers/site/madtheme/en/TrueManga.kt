package com.kosen.reader.parsers.site.madtheme.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madtheme.MadthemeParser

@MangaSourceParser("TRUEMANGA", "TrueManga", "en")
internal class TrueManga(context: MangaLoaderContext) :
	MadthemeParser(context, MangaParserSource.TRUEMANGA, "mangamonk.com")
