package com.kosen.reader.parsers.site.fuzzydoodle.fr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaState
import com.kosen.reader.parsers.site.fuzzydoodle.FuzzyDoodleParser
import java.util.*

@MangaSourceParser("LELSCANVF", "LelScanFr", "fr")
internal class LelScanVf(context: MangaLoaderContext) :
	FuzzyDoodleParser(context, MangaParserSource.LELSCANVF, "lelscanfr.com") {

	override val ongoingValue = "en-cours"
	override val finishedValue = "termin"

	override suspend fun getFilterOptions() = super.getFilterOptions().copy(
		availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED),
	)
}
