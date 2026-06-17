package com.kosen.reader.parsers.site.mangareader.th

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("TOOMTAMMANGA", "ToomtamManga", "th", ContentType.HENTAI)
internal class ToomtamManga(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.TOOMTAMMANGA,
		"toomtam-manga.com",
		pageSize = 30,
		searchPageSize = 28,
	) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
