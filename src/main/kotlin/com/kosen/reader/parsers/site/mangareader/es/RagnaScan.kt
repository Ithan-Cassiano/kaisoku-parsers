package com.kosen.reader.parsers.site.mangareader.es

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("RAGNASCAN", "RagnaScan", "es")
internal class RagnaScan(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.RAGNASCAN, "ragnascan.com", pageSize = 5, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
