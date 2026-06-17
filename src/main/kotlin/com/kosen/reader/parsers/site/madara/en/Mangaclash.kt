package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGACLASH", "ToonClash", "en")
internal class Mangaclash(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGACLASH, "toonclash.com", pageSize = 18) {
	override val datePattern = "MM/dd/yyyy"
	override val withoutAjax = true
}
