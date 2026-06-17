package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken("Site changed to Next.js - incompatible with MangaReader parser")
@MangaSourceParser("NIGHTSCANS", "NightScans", "en")
internal class Nightscans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.NIGHTSCANS, "qiscans.org", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/series"
	override val selectMangaListImg = "img.ts-post-image, picture img"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
