package com.kosen.reader.parsers.site.madara.vi

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentRating
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaListFilter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaState
import com.kosen.reader.parsers.model.SortOrder
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.oneOrThrowIfMany
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.urlEncoded

@MangaSourceParser("HENTAIVNPLUS", "HentaiVN.plus", "vi", ContentType.HENTAI)
internal class HentaiVnPlus(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HENTAIVNPLUS, "hentaivn.party", 24) {
	override val listUrl = "truyen-hentai/"
	override val tagPrefix = "the-loai/"
	override val datePattern = "dd/MM/yyyy"
	override val authorSearchSupported = true

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val pages = page + 1

		val url = buildString {
			if (!filter.author.isNullOrEmpty()) {
				clear()
				append("https://")
				append(domain)
				append("/tac-gia/")
				append(filter.author.lowercase().replace(" ", "-"))

				if (pages > 1) {
					append("/page/")
					append(pages.toString())
				}

				append("/?m_orderby=")
				when (order) {
					SortOrder.POPULARITY -> append("views")
					SortOrder.UPDATED -> append("latest")
					SortOrder.NEWEST -> append("new-manga")
					SortOrder.ALPHABETICAL -> {}
					SortOrder.RATING -> append("trending")
					SortOrder.RELEVANCE -> {}
					else -> append("latest") // default
				}
				return@buildString
			}

			append("https://")
			append(domain)

			if (pages > 1) {
				append("/page/")
				append(pages.toString())
			}

			append("/?s=")

			filter.query?.let {
				append(filter.query.urlEncoded())
			}

			append("&post_type=wp-manga")

			if (filter.tags.isNotEmpty()) {
				filter.tags.forEach {
					append("&genre[]=")
					append(it.key)
				}
			}

			filter.states.forEach {
				append("&status[]=")
				when (it) {
					MangaState.ONGOING -> append("on-going")
					MangaState.FINISHED -> append("end")
					MangaState.ABANDONED -> append("canceled")
					MangaState.PAUSED -> append("on-hold")
					MangaState.UPCOMING -> append("upcoming")
					else -> throw IllegalArgumentException("$it not supported")
				}
			}

			filter.contentRating.oneOrThrowIfMany()?.let {
				append("&adult=")
				append(
					when (it) {
						ContentRating.SAFE -> "0"
						ContentRating.ADULT -> "1"
						else -> ""
					},
				)
			}

			if (filter.year != 0) {
				append("&release=")
				append(filter.year.toString())
			}

			append("&m_orderby=")
			when (order) {
				SortOrder.POPULARITY -> append("views")
				SortOrder.UPDATED -> append("latest")
				SortOrder.NEWEST -> append("new-manga")
				SortOrder.ALPHABETICAL -> append("alphabet")
				SortOrder.RATING -> append("rating")
				SortOrder.RELEVANCE -> {}
				else -> {}
			}
		}
		return parseMangaList(webClient.httpGet(url).parseHtml())
	}
}
