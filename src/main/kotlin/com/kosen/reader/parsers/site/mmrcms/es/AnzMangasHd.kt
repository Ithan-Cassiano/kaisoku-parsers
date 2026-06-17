package com.kosen.reader.parsers.site.mmrcms.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@MangaSourceParser("ANZMANGASHD", "AnzMangasHd", "es")
internal class AnzMangasHd(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.ANZMANGASHD, "www.anzmangashd.com") {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val selectState = "dt:contains(Estado)"
	override val selectAlt = "dt:contains(Otros nombres)"
	override val selectAut = "dt:contains(Autor(es))"
	override val selectTag = "dt:contains(Categorías)"
}
