package com.kosen.reader.parsers.site.madara.pt

import org.jsoup.nodes.Element
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaListFilter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.model.SortOrder
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.mapNotNullToSet
import com.kosen.reader.parsers.util.oneOrThrowIfMany
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.toTitleCase

@MangaSourceParser("HIPERCOOL", "Hipercool", "pt", ContentType.HENTAI)
internal class Hipercool(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HIPERCOOL, "hiper.cool", pageSize = 20) {

	override val tagPrefix = "manga-genre/"
	override val stylePage = ""
	override val withoutAjax = true

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter) =
		if (needsSearchEndpoint(filter)) {
			super.getListPage(page, order, filter)
		} else {
			parseMangaList(webClient.httpGet(buildArchiveUrl(page, filter)).parseHtml())
		}

	override suspend fun fetchAvailableTags(): Set<MangaTag> {
		val fromDefault = runCatching { super.fetchAvailableTags() }.getOrDefault(emptySet())
		if (fromDefault.isNotEmpty()) {
			return fromDefault
		}
		val doc = webClient.httpGet("https://$domain/$listUrl").parseHtml()
		return doc.select("a[href*='/$tagPrefix']").mapNotNullToSet { a ->
			parseGenreTag(a)
		}
	}

	private fun parseGenreTag(a: Element): MangaTag? {
		val href = a.attr("href").removeSuffix("/")
		val key = href.substringAfterLast(tagPrefix, "")
		if (key.isEmpty() || key.contains('/')) {
			return null
		}
		val title = a.text().ifBlank { a.attr("title") }.ifBlank { return null }
		return MangaTag(
			key = key,
			title = title.toTitleCase(sourceLocale),
			source = source,
		)
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
