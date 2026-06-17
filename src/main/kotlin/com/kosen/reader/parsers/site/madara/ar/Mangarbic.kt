package com.kosen.reader.parsers.site.madara.ar

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken("Website is down or domain has been changed")
@MangaSourceParser("MANGARBIC", "MangaArabic", "ar")
internal class Mangarbic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGARBIC, "lekmanga.net") {
	override val postReq = true
	override val datePattern = "yyyy/MM/dd"
}
