package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.SortOrder
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.oneOrThrowIfMany
import com.kosen.reader.parsers.util.parseHtml

@MangaSourceParser("HANAMI_HEAVEN", "Hanami Heaven", "pt", ContentType.HENTAI)
internal class HanamiHeaven(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HANAMI_HEAVEN, "hanamiheaven.org", pageSize = 12) {

	override val tagPrefix = "manga-genre/"
	override val withoutAjax = true
	override val datePattern = "dd/MM/yyyy"

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter) =
		if (needsSearchEndpoint(filter)) {
			super.getListPage(page, order, filter)
		} else {
			parseMangaList(webClient.httpGet(buildArchiveUrl(page, filter)).parseHtml())
		}

	private fun needsSearchEndpoint(filter: MangaListFilter): Boolean =
		!filter.query.isNullOrEmpty() ||
			filter.states.isNotEmpty() ||
			filter.contentRating != null ||
			filter.year != 0 ||
			!filter.author.isNullOrEmpty()

	private fun buildArchiveUrl(page: Int, filter: MangaListFilter): String {
		val pageNum = page + 1
		return buildString {
			append("https://")
			append(domain)
			append('/')
			if (filter.tags.isNotEmpty()) {
				append(tagPrefix)
				append(filter.tags.oneOrThrowIfMany()?.key)
				append('/')
			} else {
				append(listUrl)
			}
			if (pageNum > 1) {
				append("page/")
				append(pageNum)
				append('/')
			}
		}
	}
}
