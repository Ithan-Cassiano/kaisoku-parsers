package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("GDSCANS", "GdScans", "en")
internal class GdScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GDSCANS, "gdscans.com", 10) {
	override val tagPrefix = "webtoon-genre/"
}
