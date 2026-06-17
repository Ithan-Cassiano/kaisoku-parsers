package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("WHALEMANGA", "WhaleManga", "en")
internal class WhaleManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.WHALEMANGA, "whalemanga.com", 10)
