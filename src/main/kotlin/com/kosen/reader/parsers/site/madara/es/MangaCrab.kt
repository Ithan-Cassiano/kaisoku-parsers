package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGA_CRAB", "MangaCrab", "es")
internal class MangaCrab(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA_CRAB, "mangacrab.org") {
	override val datePattern = "dd/MM/yyyy"
	override val tagPrefix = "manga-genero/"
	override val listUrl = "series/"
	override val selectChapter = "div.listing-chapters_wrap > ul > li"
	override val selectDesc = "div.c-page__content div.modal-contenido p"
	override val selectState = "div.summary-content2"
}
