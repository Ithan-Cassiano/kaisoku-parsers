package com.kosen.reader.parsers.site.zeistmanga.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaState
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser
import com.kosen.reader.parsers.util.*
import java.util.*

@MangaSourceParser("MANGAHUB_LINK", "MangaHub.link", "ar", ContentType.HENTAI)
internal class MangaHub(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.MANGAHUB_LINK, "www.mangahub.link") {

	override val sateOngoing: String = "مستمر"
	override val sateFinished: String = "مكتمل"

	override suspend fun getFilterOptions() = super.getFilterOptions().copy(
		availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED),
	)

	override suspend fun fetchAvailableTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain").parseHtml()
		return doc.requireElementById("Genre").select("div.items-center").mapToSet {
			MangaTag(
				key = it.selectFirstOrThrow("input").attr("value"),
				title = it.selectFirstOrThrow("label").text().substringBefore(')').toTitleCase(sourceLocale),
				source = source,
			)
		}
	}
}
