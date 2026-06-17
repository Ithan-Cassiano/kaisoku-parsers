package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("SHOJOSCANS", "ShojoScans", "en")
internal class ShojoScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SHOJOSCANS, "violetscans.com", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/comics"
}
