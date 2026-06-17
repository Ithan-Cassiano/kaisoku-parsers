package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MIHENTAI", "MiHentai", "id", ContentType.HENTAI)
internal class MiHentai(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MIHENTAI, "mihentai.com", pageSize = 30, searchPageSize = 10) {
	override val datePattern = "MMM d, yyyy"
	override val selectMangaList = ".listupd .bs .bsx"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
