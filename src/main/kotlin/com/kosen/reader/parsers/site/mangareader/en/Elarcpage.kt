package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("ELARCPAGE", "ElarcPage", "en")
internal class Elarcpage(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.ELARCPAGE, "elarctoon.com", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/series"
}
