package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("KABUSMANGA", "KabusManga", "tr")
internal class KabusManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KABUSMANGA, "kabusmanga.com") {
	override val datePattern = "dd/MM/yyyy"
}
