package com.kosen.reader.parsers.site.mangareader.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("GAIATOON", "GaiaToon", "tr")
internal class GaiaToon(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.GAIATOON, "gaiatoon.com", pageSize = 50, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isMultipleTagsSupported = false,
		)
}
