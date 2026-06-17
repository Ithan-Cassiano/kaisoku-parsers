package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHWAFULL", "ManhwaFull", "en")
internal class ManhwaFull(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWAFULL, "manhwafull.com") {
	override val listUrl = "manga-all-manhwa/"
	override val datePattern = "MM/dd/yyyy"
}
