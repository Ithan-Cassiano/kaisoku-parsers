package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAMANIACS", "MangaManiacs", "en", ContentType.HENTAI)
internal class MangaManiacs(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAMANIACS, "mangamaniacs.org", 10)
