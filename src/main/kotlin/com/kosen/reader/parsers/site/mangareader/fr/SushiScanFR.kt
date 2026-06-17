package com.kosen.reader.parsers.site.mangareader.fr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("SUSHISCANFR", "SushiScan.fr", "fr")
internal class SushiScanFR(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.SUSHISCANFR, "sushiscan.fr", pageSize = 36, searchPageSize = 10) {
	override val listUrl = "/catalogue"
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
