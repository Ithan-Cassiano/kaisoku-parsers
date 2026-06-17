package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANHWAFREAKE", "ManhwaFreake", "en")
internal class ManhwaFreake(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANHWAFREAKE, "manhwafreake.com", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/series"

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
