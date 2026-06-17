package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("JIMANGA", "S2Manga.io", "en")
internal class JiManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.JIMANGA, "s2manga.io")
