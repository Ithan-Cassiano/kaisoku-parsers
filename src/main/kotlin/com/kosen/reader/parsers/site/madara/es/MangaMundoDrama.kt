package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAMUNDODRAMA", "InmortalScan", "es")
internal class MangaMundoDrama(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAMUNDODRAMA, "scaninmortal.com") {
	override val listUrl = "mg/"
}
