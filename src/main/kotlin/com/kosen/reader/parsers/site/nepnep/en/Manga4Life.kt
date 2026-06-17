package com.kosen.reader.parsers.site.nepnep.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.nepnep.NepnepParser

// site closed in favour of weeb central
@Broken
@MangaSourceParser("MANGA4LIFE", "Manga4Life", "en")
internal class Manga4Life(context: MangaLoaderContext) :
	NepnepParser(context, MangaParserSource.MANGA4LIFE, "manga4life.com")
