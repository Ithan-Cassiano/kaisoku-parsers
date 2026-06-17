package com.kosen.reader.parsers.site.mangareader.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("SENPAIEDICIONES", "SenpaiEdiciones", "es")
internal class Senpaiediciones(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.SENPAIEDICIONES,
		"senpaiediciones.com",
		pageSize = 20,
		searchPageSize = 20,
	) {
	override val datePattern = "MMM d, yyyy"
}
