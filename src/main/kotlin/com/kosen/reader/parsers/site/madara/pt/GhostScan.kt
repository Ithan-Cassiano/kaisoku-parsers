package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("GHOSTSCAN", "GhostScan", "pt")
internal class GhostScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GHOSTSCAN, "ghostscan.xyz", 24) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}
