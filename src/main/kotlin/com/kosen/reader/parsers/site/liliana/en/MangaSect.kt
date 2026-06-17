package com.kosen.reader.parsers.site.liliana.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.liliana.LilianaParser

@MangaSourceParser("MANGASECT", "MangaSect", "en")
internal class MangaSect(context: MangaLoaderContext) :
	LilianaParser(context, MangaParserSource.MANGASECT, "mangasect.net")
