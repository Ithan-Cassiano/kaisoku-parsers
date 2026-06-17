package com.kosen.reader.parsers.site.zeistmanga.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaListFilterOptions
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaState
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser
import java.util.EnumSet

@MangaSourceParser("SOLOOSCAN", "SolooScan", "pt")
internal class SolooScan(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.SOLOOSCAN, "solooscan.blogspot.com") {
	override val mangaCategory = "Comics"
	override val sateOngoing: String = "Lançando"
	override val sateFinished: String = "Completo"
	override val sateAbandoned: String = "Dropado"

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED, MangaState.ABANDONED),
	)
}
