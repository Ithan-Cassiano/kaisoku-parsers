package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.Broken
import java.util.*

@Broken // The site's servers are not responding; it may be closed.
@MangaSourceParser("KOMIKLOVERS", "KomikLovers", "id")
internal class KomikLovers(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKLOVERS, "komiklovers.com", pageSize = 20, searchPageSize = 10) {
	override val sourceLocale: Locale = Locale.ENGLISH
	override val listUrl = "/komik"
}
