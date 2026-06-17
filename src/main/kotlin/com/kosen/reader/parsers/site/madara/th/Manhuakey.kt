package com.kosen.reader.parsers.site.madara.th

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentRating
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaListFilterOptions
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.site.madara.MadaraParser
import java.util.EnumSet
import java.util.Locale

@MangaSourceParser("MANHUAKEY", "ManhuaKey", "th")
internal class Manhuakey(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHUAKEY, "www.manhuakey.com", 10) {

	override val datePattern: String = "d MMMM yyyy"
	override val sourceLocale: Locale = Locale.ENGLISH
	override val withoutAjax = true
	override val selectPage = "div.text-center"

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableContentRating = EnumSet.of(ContentRating.SAFE, ContentRating.ADULT),
	)

	override suspend fun fetchAvailableTags(): Set<MangaTag> = emptySet()
}
