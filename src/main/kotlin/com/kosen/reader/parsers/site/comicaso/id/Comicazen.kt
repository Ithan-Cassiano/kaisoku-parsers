package com.kosen.reader.parsers.site.comicaso.id

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.comicaso.ComicasoParser

@Broken
@MangaSourceParser("COMICAZEN", "Comicazen", "id")
internal class Comicazen(context: MangaLoaderContext) :
	ComicasoParser(context, MangaParserSource.COMICAZEN, "comicazen.com", pageSize = 16)
