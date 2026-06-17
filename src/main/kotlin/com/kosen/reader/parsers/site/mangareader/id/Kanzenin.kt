package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("KANZENIN", "Kanzenin", "id", ContentType.HENTAI)
internal class Kanzenin(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KANZENIN, "kanzenin.info", pageSize = 27, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
