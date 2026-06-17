package com.kosen.reader.parsers.site.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource

@MangaSourceParser("PLUMACOMICS", "PlumaComics", "pt")
internal class PlumaComics(context: MangaLoaderContext) :
	PlumaComicsParser(context, MangaParserSource.PLUMACOMICS, "plumacomics.cloud")
