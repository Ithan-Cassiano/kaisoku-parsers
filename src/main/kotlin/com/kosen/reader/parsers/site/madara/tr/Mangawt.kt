package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAWT", "MangaWt.com", "tr")
internal class Mangawt(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAWT, "mangawt.com")
