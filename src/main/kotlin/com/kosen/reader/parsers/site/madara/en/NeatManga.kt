package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("NEATMANGA", "NeatManga", "en")
internal class NeatManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NEATMANGA, "neatmangas.com", pageSize = 20) {
	override val datePattern = "dd MMMM yyyy"
}
