package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("NOVELMIC", "NovelMic", "en")
internal class Novelmic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NOVELMIC, "novelmic.com", 20) {
	override val postReq = true
}
