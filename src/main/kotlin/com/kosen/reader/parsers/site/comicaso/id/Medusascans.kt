package com.kosen.reader.parsers.site.comicaso.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.comicaso.ComicasoParser

@MangaSourceParser("MEDUSASCANS", "Medusascans", "id", ContentType.HENTAI)
internal class Medusascans(context: MangaLoaderContext) :
	ComicasoParser(context, MangaParserSource.MEDUSASCANS, "medusascans.com", pageSize = 16)
