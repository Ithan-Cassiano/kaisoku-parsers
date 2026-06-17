package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LMTOS", "Lmtos", "es")
internal class Lmtos(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LMTOS, "lmtos.com")
