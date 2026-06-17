package com.kosen.reader.parsers.site.onemanga.fr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.onemanga.OneMangaParser

@MangaSourceParser("DANDADAN", "Dandadan", "fr")
internal class Dandadan(context: MangaLoaderContext) :
	OneMangaParser(context, MangaParserSource.DANDADAN, "dandadan.fr")
