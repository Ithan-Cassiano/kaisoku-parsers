package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAGEZGINI", "MangaGezgini", "tr")
internal class MangaGezgini(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAGEZGINI, "mangagezginim.com", pageSize = 20) {
	override val datePattern = "dd/MM/yyyy"
}
