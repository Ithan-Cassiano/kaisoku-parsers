package com.kosen.reader.parsers.site.guya.all

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.guya.GuyaParser

@MangaSourceParser("MAHOUSHOUJOBU", "MahouShoujobu")
internal class MahouShoujobu(context: MangaLoaderContext) :
	GuyaParser(context, MangaParserSource.MAHOUSHOUJOBU, "mahoushoujobu.com")
