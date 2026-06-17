package com.kosen.reader.parsers.site.mangareader.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.Broken

@Broken
@MangaSourceParser("SKYMANGAS", "SkyMangas", "es")
internal class SkyMangas(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SKYMANGAS, "skymangas.com", pageSize = 20, searchPageSize = 10) {
	override val encodedSrc = true
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
