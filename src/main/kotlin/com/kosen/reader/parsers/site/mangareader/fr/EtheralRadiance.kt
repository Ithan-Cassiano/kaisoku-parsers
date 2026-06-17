package com.kosen.reader.parsers.site.mangareader.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.util.insertCookies

@Broken
@MangaSourceParser("ETHERALRADIANCE", "EtheralRadiance", "fr")
internal class EtheralRadiance(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.ETHERALRADIANCE,
		"www.etheralradiance.eu",
		pageSize = 20,
		searchPageSize = 10,
	) {

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)

	init {
		context.cookieJar.insertCookies(
			domain,
			"_lscache_vary=1;",
		)
	}
}
