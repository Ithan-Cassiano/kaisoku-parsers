package com.kosen.reader.parsers.site.mangareader.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.Broken

@Broken // There are no comics on the website at all.
@MangaSourceParser("GREMORYMANGAS", "GremoryMangas", "es")
internal class GremoryMangas(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.GREMORYMANGAS,
		"gremorymangas.com",
		pageSize = 20,
		searchPageSize = 20,
	) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
