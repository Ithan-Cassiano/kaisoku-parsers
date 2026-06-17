package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaListFilterOptions
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("SKY_MANGA", "SkyManga", "en", ContentType.HENTAI)
internal class SkyManga(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SKY_MANGA, "skymanga.work", pageSize = 20, searchPageSize = 20) {
	override val listUrl = "/manga-list"
	override val datePattern = "dd-MM-yyyy"

	// Server ignores ?s=, genre[], type=, status= — only order= and page= are honoured.
	// Verified 2026-04-24 against live skymanga.work: every search query returns the same 7
	// featured titles; every genre/type/status value returns the same 30 titles as the default
	// sort. See PR body for the probe details.
	override val filterCapabilities = MangaListFilterCapabilities()

	override suspend fun getFilterOptions() = MangaListFilterOptions()
}
