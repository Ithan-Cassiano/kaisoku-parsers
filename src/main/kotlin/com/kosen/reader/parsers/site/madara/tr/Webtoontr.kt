package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("WEBTOONTR", "WebtoonTr", "tr")
internal class Webtoontr(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.WEBTOONTR, "webtoontr.net", 16) {
	override val tagPrefix = "webtoon-kategori/"
	override val listUrl = "webtoon/"
	override val datePattern = "dd/MM/yyyy"
}
