package com.kosen.reader.parsers.site.zeistmanga.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("XSANOMANGA", "XsanoManga", "ar")
internal class XsanoManga(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.XSANOMANGA, "www.xsano-manga.com")
