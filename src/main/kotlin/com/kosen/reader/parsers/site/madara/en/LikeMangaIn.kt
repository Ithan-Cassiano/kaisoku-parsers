package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("LIKEMANGAIN", "LikeManga.in", "en")
internal class LikeMangaIn(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LIKEMANGAIN, "likemanga.in", 36) {
	override val datePattern = "d MMMM, yyyy"
}
