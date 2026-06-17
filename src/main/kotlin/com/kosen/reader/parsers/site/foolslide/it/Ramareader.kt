package com.kosen.reader.parsers.site.foolslide.it

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.foolslide.FoolSlideParser

@MangaSourceParser("RAMAREADER", "RamaReader", "it")
internal class Ramareader(context: MangaLoaderContext) :
	FoolSlideParser(context, MangaParserSource.RAMAREADER, "www.ramareader.it") {
	override val searchUrl = "read/search/"
	override val listUrl = "read/directory/"
}
