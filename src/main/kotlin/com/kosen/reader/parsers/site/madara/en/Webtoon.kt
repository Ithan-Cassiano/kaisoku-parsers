package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("WEBTOON", "Webtoon.uk", "en")
internal class Webtoon(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.WEBTOON, "webtoon.uk", 20) {
	override val tagPrefix = "manhwa-genre/"
	override val postReq = true
}
