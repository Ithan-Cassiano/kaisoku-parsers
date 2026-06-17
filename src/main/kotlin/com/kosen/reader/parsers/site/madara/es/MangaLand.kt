package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGALAND", "MangaLand", "es", ContentType.HENTAI)
internal class MangaLand(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGALAND, "mangaland.net")
