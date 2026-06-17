package com.kosen.reader.parsers.site.fuzzydoodle.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilterOptions
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.fuzzydoodle.FuzzyDoodleParser
import java.util.EnumSet

@MangaSourceParser("SCYLLACOMICS", "ScyllaComics", "en")
internal class ScyllaComics(context: MangaLoaderContext) :
	FuzzyDoodleParser(context, MangaParserSource.SCYLLACOMICS, "scyllacomics.xyz") {

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableContentTypes = EnumSet.of(
			ContentType.MANGA,
			ContentType.MANHWA,
			ContentType.MANHUA,
		),
	)
}
