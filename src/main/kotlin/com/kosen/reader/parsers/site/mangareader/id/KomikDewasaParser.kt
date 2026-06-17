package com.kosen.reader.parsers.site.mangareader.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import java.util.*

@MangaSourceParser("KOMIKDEWASA", "komikRemaja.icu", "id", ContentType.HENTAI)
internal class KomikDewasaParser(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.KOMIKDEWASA, "komikremaja.icu", pageSize = 20, searchPageSize = 10) {
	override val listUrl: String = "/komik"
	override val sourceLocale: Locale = Locale.ENGLISH
	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isTagsExclusionSupported = false,
		)
}
