package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAREAD", "MangaRead", "en")
internal class MangaRead(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAREAD, "www.mangaread.org") {
	override val tagPrefix = "genres/"
	override val datePattern = "dd.MM.yyyy"
}
