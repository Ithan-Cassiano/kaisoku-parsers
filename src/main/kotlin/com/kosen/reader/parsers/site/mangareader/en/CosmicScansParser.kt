package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("COSMICSCANS", "CosmicScans.com", "en")
internal class CosmicScansParser(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.COSMICSCANS, "cosmic-scans.com", pageSize = 20, searchPageSize = 10) {
	override val datePattern = "MMM d, yyyy"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
