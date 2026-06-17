package com.kosen.reader.parsers.site.zeistmanga.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("NGAMENKOMIK", "NgamenKomik", "id")
internal class NgamenKomik(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.NGAMENKOMIK, "ngamenkomik05.blogspot.com")
