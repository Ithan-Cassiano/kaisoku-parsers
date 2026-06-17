package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("FREAKSCANS", "FreakScans", "en")
internal class FreakScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.FREAKSCANS, "freakscans.com", pageSize = 20, searchPageSize = 20) {
	override val datePattern = "MMM d, yyyy"

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
