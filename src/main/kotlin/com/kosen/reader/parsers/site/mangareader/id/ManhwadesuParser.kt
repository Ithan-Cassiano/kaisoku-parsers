package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANHWADESU", "ManhwaDesu", "id", ContentType.HENTAI)
internal class ManhwadesuParser(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANHWADESU, "manhwadesu.asia", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/komik"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
