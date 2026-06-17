package com.kosen.reader.parsers.site.animebootstrap.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.animebootstrap.AnimeBootstrapParser

@MangaSourceParser("NEUMANGA", "NeuManga.xyz", "id")
internal class NeuManga(context: MangaLoaderContext) :
	AnimeBootstrapParser(context, MangaParserSource.NEUMANGA, "neumanga.xyz")
