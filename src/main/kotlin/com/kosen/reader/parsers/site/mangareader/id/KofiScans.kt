package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import java.util.*

@MangaSourceParser("KOFISCANS", "KofiScans", "id")
internal class KofiScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOFISCANS, "isekaikomik.com", pageSize = 20, searchPageSize = 10) {
	override val sourceLocale: Locale = Locale.ENGLISH
}
