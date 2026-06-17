package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("JIANGZAITOON", "JiangzaiToon", "tr", ContentType.HENTAI)
internal class Jiangzaitoon(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.JIANGZAITOON, "jiangzaitoon.top") {
	override val datePattern = "d MMMM yyyy"
	override val postReq = true
}
