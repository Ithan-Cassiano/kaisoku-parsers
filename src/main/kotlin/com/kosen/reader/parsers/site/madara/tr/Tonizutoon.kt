package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TONIZUTOON", "ToniZu.com", "tr", ContentType.HENTAI)
internal class Tonizutoon(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TONIZUTOON, "tonizu.top") {
	override val datePattern = "dd/mm/yyyy"
}
