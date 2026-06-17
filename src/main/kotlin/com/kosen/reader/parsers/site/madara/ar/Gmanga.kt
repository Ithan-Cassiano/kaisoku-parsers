package com.kosen.reader.parsers.site.madara.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("GMANGA", "Gmanga", "ar")
internal class Gmanga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GMANGA, "gmanga.site")
