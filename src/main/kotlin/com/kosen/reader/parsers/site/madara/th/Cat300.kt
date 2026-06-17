package com.kosen.reader.parsers.site.madara.th

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("CAT_300", "Cat300", "th", ContentType.HENTAI)
internal class Cat300(context: MangaLoaderContext) : MadaraParser(context, MangaParserSource.CAT_300, "cat-300.com") {
	override val datePattern = "MMMM dd, yyyy"
}
