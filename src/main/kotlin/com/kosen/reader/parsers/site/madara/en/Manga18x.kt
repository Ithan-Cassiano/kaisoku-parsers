package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGA18X", "Manga18x", "en", ContentType.HENTAI)
internal class Manga18x(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA18X, "manga18x.net", 24)
