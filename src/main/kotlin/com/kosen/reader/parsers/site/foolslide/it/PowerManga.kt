package com.kosen.reader.parsers.site.foolslide.it

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.foolslide.FoolSlideParser

@Broken
@MangaSourceParser("POWERMANGA", "PowerManga", "it")
internal class PowerManga(context: MangaLoaderContext) :
	FoolSlideParser(context, MangaParserSource.POWERMANGA, "reader.powermanga.org") {
	override val pagination = false
}
