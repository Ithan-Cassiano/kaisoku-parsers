package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TIMENAIGHT", "TimeNaight", "tr")
internal class Timenaight(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TIMENAIGHT, "timenaight.org") {
	override val postReq = true
	override val datePattern = "dd/MM/yyyy"
}
