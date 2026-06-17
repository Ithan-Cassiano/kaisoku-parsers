package com.kosen.reader.parsers.site.mangareader.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.Broken

@Broken
@MangaSourceParser("ROBINMANGA", "RobinManga", "tr")
internal class RobinManga(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.ROBINMANGA,
		"www.guildknives.com",
		pageSize = 20,
		searchPageSize = 25,
	) {
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
