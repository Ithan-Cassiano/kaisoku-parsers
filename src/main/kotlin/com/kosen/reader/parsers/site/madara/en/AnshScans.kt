package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("ANSHSCANS", "AnshScans", "en")
internal class AnshScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ANSHSCANS, "anshscans.org", 10) {
	override val tagPrefix = "genre/"
	override val datePattern = "MMMM dd, yyyy"
}
