package com.kosen.reader.parsers.site.zeistmanga.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("ELEVENSCANLATOR", "ElevenScanlator", "pt")
internal class ElevenScanlator(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.ELEVENSCANLATOR, "elevenscanlator.blogspot.com") {
	override val sateOngoing: String = "Lançando"
	override val sateFinished: String = "Completo"
	override val sateAbandoned: String = "Dropado"
}
