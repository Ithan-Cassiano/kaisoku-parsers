package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("KOMIKSTATION", "KomikStation", "id")
internal class Komikstation(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKSTATION, "komikstation.org", pageSize = 30, searchPageSize = 30) {
	override val datePattern = "MMM d, yyyy"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
