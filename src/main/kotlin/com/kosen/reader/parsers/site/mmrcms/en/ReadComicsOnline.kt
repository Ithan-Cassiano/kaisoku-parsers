package com.kosen.reader.parsers.site.mmrcms.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mmrcms.MmrcmsParser

@MangaSourceParser("READCOMICSONLINE", "ReadComicsOnline.ru", "en", ContentType.COMICS)
internal class ReadComicsOnline(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.READCOMICSONLINE, "readcomicsonline.ru") {
	override val selectState = "dt:contains(Status)"
	override val selectTag = "dt:contains(Categories)"
}
