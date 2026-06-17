package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("BOOKMANGA", "BookManga", "en")
internal class BookManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BOOKMANGA, "bookmanga.com", 20)
