package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MAIDSECRET", "MaidSecret", "pt")
internal class MaidSecret(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MAIDSECRET, "maidsecret.com", 10) {
	override val datePattern: String = "dd/MM/yyyy"
}
