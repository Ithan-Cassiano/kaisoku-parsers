package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("LKSCANLATION", "LkScanlation", "es")
internal class LkScanlation(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LKSCANLATION, "lkscanlation.com") {
	override val tagPrefix = "manhwa-genre/"
	override val listUrl = "manhwa/"
}
