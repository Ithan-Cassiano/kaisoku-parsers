package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MILFTOON", "MilfToon", "en", ContentType.HENTAI)
internal class MilfToon(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MILFTOON, "milftoon.xxx", 20) {
	override val postReq = true
	override val datePattern = "d MMMM, yyyy"
}
