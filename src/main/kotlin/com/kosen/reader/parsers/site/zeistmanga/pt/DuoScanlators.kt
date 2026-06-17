package com.kosen.reader.parsers.site.zeistmanga.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("DUOSCANLATORS", "DuoScanlators", "pt")
internal class DuoScanlators(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.DUOSCANLATORS, "duoscanlators.blogspot.com")
