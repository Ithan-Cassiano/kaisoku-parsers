package com.kosen.reader.parsers.site.wpcomics.vi

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaState
import com.kosen.reader.parsers.model.RATING_UNKNOWN
import com.kosen.reader.parsers.site.wpcomics.WpComicsParser
import com.kosen.reader.parsers.util.mapNotNullToSet
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.textOrNull
import com.kosen.reader.parsers.util.toAbsoluteUrl
import com.kosen.reader.parsers.Broken

@Broken // The website is not responding, it may be closed.
@MangaSourceParser("HAMTRUYEN", "Ham Truyện", "vi")
internal class HamTruyen(context: MangaLoaderContext) :
	WpComicsParser(context, MangaParserSource.HAMTRUYEN, "hamtruyen1.com", 44) {

	override suspend fun getDetails(manga: Manga): Manga = coroutineScope {
		val fullUrl = manga.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(fullUrl).parseHtml()
		val chaptersDeferred = async { getChapters(doc, reversed = true) }
		val tagMap = getOrCreateTagMap()
		val tagsElement = doc.select("li.kind p.col-xs-8 a")
		val mangaTags = tagsElement.mapNotNullToSet { tagMap[it.text()] }
		val author = doc.body().select(selectAut).textOrNull()
		manga.copy(
			description = doc.selectFirst(selectDesc)?.html(),
			altTitles = setOfNotNull(doc.selectFirst("h2.other-name")?.textOrNull()),
			authors = setOfNotNull(author),
			state = doc.selectFirst(selectState)?.let {
				when (it.text()) {
					in ongoing -> MangaState.ONGOING
					in finished -> MangaState.FINISHED
					else -> null
				}
			},
			tags = mangaTags,
			rating = doc.selectFirst("div.star input")?.attr("value")?.toFloatOrNull()?.div(5f) ?: RATING_UNKNOWN,
			chapters = chaptersDeferred.await(),
		)
	}
}
