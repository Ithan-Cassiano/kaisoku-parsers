package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANHWALAND_INK", "ManhwaLand.ink", "id", ContentType.HENTAI)
internal class ManhwaLandInk(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.MANHWALAND_INK,
		"manhwaland.asia",
		pageSize = 20,
		searchPageSize = 10,
	) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
	override val datePattern = "MMM d, yyyy"
}
