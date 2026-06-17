package com.kosen.reader.parsers.site.mangareader.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("AYATOON", "AyaToon", "tr")
internal class Ayatoon(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.AYATOON, "ayatoon.com", pageSize = 20, searchPageSize = 20) {

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
