package com.kosen.reader.parsers.site.mangareader.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("VFSCAN", "VfScan", "fr")
internal class VfScan(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.VFSCAN, "www.vfscan.net", pageSize = 18, searchPageSize = 18) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
