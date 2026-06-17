package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("PIEDPIPERFANSUBYY", "PiedPiperFansubyy", "tr", ContentType.HENTAI)
internal class PiedPiperFansubyy(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PIEDPIPERFANSUBYY, "piedpiperfansubyy.me", 18) {
	override val datePattern = "d MMMM yyyy"
}
