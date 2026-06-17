package com.kosen.reader.parsers.site.zeistmanga.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser
import com.kosen.reader.parsers.util.mapToSet
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.selectFirstOrThrow

@MangaSourceParser("MIKROKOSMOSFB", "Mikrokosmosfb", "tr", ContentType.HENTAI)
internal class Mikrokosmosfb(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.MIKROKOSMOSFB, "mikrokosmosfb.blogspot.com") {
	override val sateOngoing: String = "Devam Ediyor"
	override val sateFinished: String = "Tamamlandı"
	override val sateAbandoned: String = "Güncel"

	override suspend fun fetchAvailableTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain").parseHtml()
		val tags = doc.selectFirstOrThrow("script:containsData(label: )").data()
			.substringAfter("label: [").substringBefore("]").replace("\"", "").split(", ")
		return tags.mapToSet {
			MangaTag(
				key = it,
				title = it,
				source = source,
			)
		}
	}
}
