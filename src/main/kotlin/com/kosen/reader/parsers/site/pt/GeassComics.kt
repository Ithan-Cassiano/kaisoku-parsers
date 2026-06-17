package com.kosen.reader.parsers.site.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource

@MangaSourceParser("GEASSCOMICS", "Geass Comics", "pt")
internal class GeassComics(context: MangaLoaderContext) :
	SkkyScanParser(context, MangaParserSource.GEASSCOMICS, "geasscomics.xyz")
