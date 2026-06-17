package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("LINKSTARTSCAN", "LinkStartScan", "pt")
internal class LinkStartScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LINKSTARTSCAN, "www.linkstartscan.xyz") {
	override val datePattern: String = "dd/MM/yyyy"
}
