package com.kosen.reader.parsers.site.madara.ru

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAMAMMY", "MangaMammy", "ru")
internal class MangaMammy(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAMAMMY, "mangamammy.ru") {
	override val datePattern = "dd.MM.yyyy"
	override val postReq = true
}
