package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("KOMIKLOKAL", "KomikLokal", "id", ContentType.HENTAI)
internal class KomikLokalParser(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKLOKAL, "komikmu.top", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/komik"
	override val datePattern = "dd/MM/yyyy"
}
