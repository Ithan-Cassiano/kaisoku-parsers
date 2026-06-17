package com.kosen.reader.parsers.site.mangareader.th

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import java.util.*

@MangaSourceParser("REAPERTRANS", "ReaperTrans", "th")
internal class ReaperTrans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.REAPERTRANS, "reapertrans.com", pageSize = 30, searchPageSize = 14) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
	override val sourceLocale: Locale = Locale.ENGLISH
}
