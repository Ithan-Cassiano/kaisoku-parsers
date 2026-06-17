package com.kosen.reader.parsers.site.foolslide.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.foolslide.FoolSlideParser

@MangaSourceParser("MENUDO_FANSUB", "Menudo Fansub", "es")
internal class MenudoFansub(context: MangaLoaderContext) :
	FoolSlideParser(context, MangaParserSource.MENUDO_FANSUB, "www.menudo-fansub.com", 25) {
	override val searchUrl = "slide/search/"
	override val listUrl = "slide/directory/"
}
