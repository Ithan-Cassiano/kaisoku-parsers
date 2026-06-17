package com.kosen.reader.parsers.site.mangareader.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("HYPERIONSCANS", "SeraphManga", "tr")
internal class HyperionScans(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.HYPERIONSCANS,
		"www.seraphmanga.com",
		pageSize = 20,
		searchPageSize = 10,
	) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
