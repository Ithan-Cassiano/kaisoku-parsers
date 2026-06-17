package com.kosen.reader.parsers.site.likemanga.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.likemanga.LikeMangaParser

@MangaSourceParser("LIKEMANGA", "LikeManga", "en")
internal class LikeManga(context: MangaLoaderContext) :
	LikeMangaParser(context, MangaParserSource.LIKEMANGA, "likemanga.ink")
