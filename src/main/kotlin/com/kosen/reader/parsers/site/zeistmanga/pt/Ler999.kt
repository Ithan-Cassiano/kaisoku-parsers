package com.kosen.reader.parsers.site.zeistmanga.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("LER999", "Ler999", "pt")
internal class Ler999(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.LER999, "ler999.blogspot.com")
