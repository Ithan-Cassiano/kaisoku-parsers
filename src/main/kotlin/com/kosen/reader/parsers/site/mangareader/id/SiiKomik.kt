package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.Broken

@Broken // The website is either closed or constantly blocked
@MangaSourceParser("SIIKOMIK", "SiiKomik", "id")
internal class SiiKomik(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SIIKOMIK, "siikomik.fun", pageSize = 20, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
			isSearchSupported = false,
		)
}

