package com.kosen.reader.parsers.site.madara.th

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("BAKAMAN", "BakaMan", "th")
internal class BakaMan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.BAKAMAN, "bakaman.net", pageSize = 18) {
	override val datePattern = "MMMM dd, yyyy"
}
