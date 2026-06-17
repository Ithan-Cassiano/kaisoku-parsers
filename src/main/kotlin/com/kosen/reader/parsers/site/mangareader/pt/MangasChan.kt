package com.kosen.reader.parsers.site.mangareader.pt

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("MANGASCHAN", "MangasChan", "pt")
internal class MangasChan(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGASCHAN, "mangaschan.net", pageSize = 20, searchPageSize = 20) {
	override val datePattern = "MMMM d, yyyy"
}
