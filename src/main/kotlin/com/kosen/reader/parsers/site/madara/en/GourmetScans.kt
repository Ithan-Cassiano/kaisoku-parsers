package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("GOURMETSCANS", "GourmetScans", "en")
internal class GourmetScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GOURMETSCANS, "gourmetsupremacy.com") {
	override val listUrl = "project/"
	override val tagPrefix = "genre/"
	override val stylePage = ""
}
