package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("THEGUILDSCANS", "TheGuildScans", "en")
internal class Theguildscans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.THEGUILDSCANS, "theguildscans.com") {
	override val postReq = true
}
