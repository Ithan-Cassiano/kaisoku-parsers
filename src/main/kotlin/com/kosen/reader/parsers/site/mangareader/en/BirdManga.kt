package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("BIRDMANGA", "BirdManga", "en")
internal class BirdManga(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.BIRDMANGA, "birdmanga.com", pageSize = 20, searchPageSize = 10) {
	override val encodedSrc = true

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
