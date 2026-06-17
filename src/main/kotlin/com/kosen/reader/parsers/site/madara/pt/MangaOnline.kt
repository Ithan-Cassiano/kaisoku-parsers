package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAONLINE_BLOG", "MangaOnline", "pt")
internal class MangaOnline(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAONLINE_BLOG, "mangaonline.blog", 16) {
	override val datePattern: String = "dd 'de' MMMMM 'de' yyyy"
}
