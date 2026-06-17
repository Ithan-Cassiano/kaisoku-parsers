package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MSYPUBLISHER", "MsyPublisher", "en")
internal class MsyPublisher(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MSYPUBLISHER, "msypublisher.com", 20) {
	override val listUrl = "manhua/"
	override val selectGenre = "manhua-genre/"
}
