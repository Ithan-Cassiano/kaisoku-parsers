package com.kosen.reader.parsers.site.mangareader.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.Broken

@Broken
@MangaSourceParser("INARIPIKAV", "InariPikav", "es")
internal class InariPikav(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.INARIPIKAV,
		"clubinari.org",
		pageSize = 10,
		searchPageSize = 10,
	) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
