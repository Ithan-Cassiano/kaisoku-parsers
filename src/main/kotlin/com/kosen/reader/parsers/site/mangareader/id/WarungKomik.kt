package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("WARUNGKOMIK", "WarungKomik", "id")
internal class WarungKomik(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.WARUNGKOMIK, "warungkomik.com", pageSize = 20, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}