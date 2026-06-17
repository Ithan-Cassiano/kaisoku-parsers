package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("ARMONISCANS", "ArmoniScans", "tr")
internal class ArmoniScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ARMONISCANS, "armoniscans.net") {
	override val tagPrefix = "tur/"
}
