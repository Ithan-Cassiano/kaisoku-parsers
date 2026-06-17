package com.kosen.reader.parsers.site.mangareader.tr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("MANGACIM", "Mangacim", "tr")
internal class Mangacim(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGACIM, "mangacim.com.tr", pageSize = 20, searchPageSize = 20) {
	override val datePattern = "MMM d, yyyy"
}
