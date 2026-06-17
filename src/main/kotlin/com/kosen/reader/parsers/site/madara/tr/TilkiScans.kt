package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TILKISCANS", "TilkiScans", "tr")
internal class TilkiScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TILKISCANS, "www.tilkiscans.com", pageSize = 18) {
	override val datePattern = "dd/MM/yyyy"
}
