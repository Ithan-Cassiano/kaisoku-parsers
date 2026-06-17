package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

// Redirect to @LUACOMIC_COM
@Broken("Server timeout - unreachable domain")
@MangaSourceParser("KAISCANS", "KaiScans", "en")
internal class KaiScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KAISCANS, "luascans.com", pageSize = 20, searchPageSize = 10) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
