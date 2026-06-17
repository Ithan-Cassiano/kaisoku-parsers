package com.kosen.reader.parsers.site.madara.th

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("DOUJINZA", "Doujinza", "th", ContentType.HENTAI)
internal class Doujinza(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.DOUJINZA, "doujinza.com", 24) {
	override val withoutAjax = true
	override val datePattern = "MMMM dd, yyyy"
	override val listUrl = "doujin/"
	override val tagPrefix = "doujin-genre/"
}
