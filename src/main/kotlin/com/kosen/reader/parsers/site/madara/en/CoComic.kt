package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("COCOMIC", "CoComic", "en", ContentType.HENTAI)
internal class CoComic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.COCOMIC, "cocomic.co") {

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
		)

	override suspend fun fetchAvailableTags(): Set<MangaTag> = emptySet()
}
