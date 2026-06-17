package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAWEEBS", "MangaWeebs", "en")
internal class MangaWeebs(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAWEEBS, "mangaweebs.in", pageSize = 20) {
	override val datePattern = "dd MMMM HH:mm"
}
