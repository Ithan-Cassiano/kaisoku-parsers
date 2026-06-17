package com.kosen.reader.parsers.site.mmrcms.id

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@Broken
@MangaSourceParser("MANGAID", "MangaId", "id")
internal class Mangaid(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.MANGAID, "mangaid.click") {
	override val selectState = "dt:contains(Status)"
	override val selectAlt = "dt:contains(Other names)"
	override val selectAut = "dt:contains(Author(s))"
	override val selectTag = "dt:contains(Categories)"
	override val sourceLocale: Locale = Locale.ENGLISH
}
