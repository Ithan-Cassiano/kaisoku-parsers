package com.kosen.reader.parsers.site.madara.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("LIKEMANGANET", "Like-Manga.net", "ar")
internal class LikeMangaNet(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LIKEMANGANET, "like-manga.net", pageSize = 10)
