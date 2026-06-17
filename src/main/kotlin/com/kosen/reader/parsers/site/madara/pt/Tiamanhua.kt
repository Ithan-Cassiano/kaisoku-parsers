package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.SortOrder
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.parseHtml

@MangaSourceParser("TIAMANHUA", "TiaManhua", "pt", ContentType.HENTAI)
internal class Tiamanhua(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TIAMANHUA, "tiamanhwa.com", pageSize = 24) {

	override val listUrl = "manhwa/"
	override val datePattern = "dd/MM/yyyy"
	override val selectChapter = "li.wp-manga-chapter, li.chapter-item, div.chapter, div.wp-manga-chapter"

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter) =
		if (shouldUseArchiveListing(filter, order)) {
			parseMangaList(
				webClient.httpGet(buildArchiveListingUrl(page)).parseHtml(),
			)
		} else {
			super.getListPage(page, order, filter)
		}

	private fun shouldUseArchiveListing(filter: MangaListFilter, order: SortOrder): Boolean =
		order == SortOrder.UPDATED &&
			filter.query.isNullOrEmpty() &&
			filter.tags.isEmpty() &&
			filter.tagsExclude.isEmpty() &&
			filter.states.isEmpty() &&
			filter.year == 0 &&
			filter.author.isNullOrEmpty() &&
			filter.contentRating == null

	private fun buildArchiveListingUrl(page: Int): String {
		val pageNum = page + 1
		return if (pageNum <= 1) {
			"https://$domain/"
		} else {
			"https://$domain/page/$pageNum/"
		}
	}
}
