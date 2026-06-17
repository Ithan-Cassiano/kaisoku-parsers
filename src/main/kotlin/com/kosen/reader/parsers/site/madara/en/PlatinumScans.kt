package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("PLATINUMSCANS", "PlatinumScans", "en")
internal class PlatinumScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PLATINUMSCANS, "platinumscans.com", pageSize = 10) {
	override val postReq = true
}
