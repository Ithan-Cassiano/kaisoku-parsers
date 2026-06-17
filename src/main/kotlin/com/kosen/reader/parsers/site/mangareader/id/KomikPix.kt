package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("KOMIKPIX", "KomikPix", "id", ContentType.HENTAI)
internal class KomikPix(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKPIX, "komikpix.com", pageSize = 30, searchPageSize = 14) {
	override val listUrl = "/hentai"
}