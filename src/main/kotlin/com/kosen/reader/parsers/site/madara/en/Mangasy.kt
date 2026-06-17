package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGASY", "Mangasy", "en")
internal class Mangasy(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGASY, "www.mangasy.com") {
	override val tagPrefix = "manhua-genre/"
}
