package com.kosen.reader.parsers.site.mangareader.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("GAFELAND", "Gafeland", "tr")
internal class Gafeland(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.GAFELAND, "www.gafeland.com", pageSize = 20, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}

