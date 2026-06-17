package com.kosen.reader.parsers.site.mangareader.th

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("POPSMANGA", "PopsManga", "th")
internal class PopsManga(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.POPSMANGA, "popsmanga.com", pageSize = 20, searchPageSize = 14) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
