package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("YAOITR", "YaoiTr", "tr")
internal class YaoiTr(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.YAOITR, "yaoitr.fun", 16) {
	override val datePattern = "d MMMM yyyy"
}
