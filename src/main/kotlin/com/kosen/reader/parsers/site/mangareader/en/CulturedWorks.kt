package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("CULTUREDWORKS", "CulturedWorks", "en")
internal class CulturedWorks(context: MangaLoaderContext) :
	MangaReaderParser(
		context,
		MangaParserSource.CULTUREDWORKS,
		"culturedworks.com",
		pageSize = 20,
		searchPageSize = 10,
	) {
	override val selectMangaList = ".listupd .bs .bsx"
}
