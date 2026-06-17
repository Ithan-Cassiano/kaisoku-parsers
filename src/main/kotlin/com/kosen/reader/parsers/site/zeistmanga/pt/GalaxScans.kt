package com.kosen.reader.parsers.site.zeistmanga.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("GALAXSCANS", "GalaxScanlator", "pt")
internal class GalaxScans(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.GALAXSCANS, "galaxscanlator.blogspot.com") {
	override val mangaCategory = "Recentes"
	override val sateOngoing: String = "Lançando"
	override val sateFinished: String = "Completo"
	override val sateAbandoned: String = "Dropado"
}
