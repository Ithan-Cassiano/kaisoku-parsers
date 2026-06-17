package com.kosen.reader.parsers.site.pt

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.core.PagedMangaParser
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.util.*
import java.util.EnumSet

@MangaSourceParser("MEGAHENTAI", "MegaHentai", "pt", ContentType.HENTAI)
internal class MegaHentai(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.MEGAHENTAI, pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("megahentai.biz")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.UPDATED)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isMultipleTagsSupported = false,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchTags(),
		availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildListUrl(page, filter)
		val useUpdatedList = filter.query.isNullOrEmpty() && filter.tags.isEmpty()
		return parseMangaList(webClient.httpGet(url).parseHtml(), useUpdatedList)
	}

	private fun buildListUrl(page: Int, filter: MangaListFilter): String = buildString {
		append("https://")
		append(domain)
		when {
			!filter.query.isNullOrEmpty() -> {
				append("/?s=")
				append(filter.query.urlEncoded())
				if (page > 1) {
					append("&paged=")
					append(page)
				}
			}

			filter.tags.isNotEmpty() -> {
				append("/genero/")
				append(filter.tags.oneOrThrowIfMany()?.key)
				append('/')
				appendPageSegment(page)
				appendStatusQuery(filter)
			}

			else -> {
				append("/capitulos-recentes/")
				appendPageSegment(page)
				appendStatusQuery(filter)
			}
		}
	}

	private fun StringBuilder.appendPageSegment(page: Int) {
		if (page > 1) {
			append("page/")
			append(page)
			append('/')
		}
	}

	private fun StringBuilder.appendStatusQuery(filter: MangaListFilter) {
		val statusParam = filter.states.oneOrThrowIfMany()?.let { state ->
			when (state) {
				MangaState.ONGOING -> "status=ativo"
				MangaState.FINISHED -> "status=completo"
				else -> return@let null
			}
		} ?: return
		append(if ('?' in this) '&' else '?')
		append(statusParam)
	}

	private suspend fun fetchTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain/capitulos-recentes/").parseHtml()
		return doc.select("a[href*=/genero/]").mapNotNullToSet { a ->
			val key = a.attr("href").removeSuffix("/").substringAfterLast('/')
			if (key.isEmpty() || key == "genero") return@mapNotNullToSet null
			MangaTag(
				key = key,
				title = a.text().ifEmpty { return@mapNotNullToSet null }.toTitleCase(),
				source = source,
			)
		}
	}

	private fun parseMangaList(doc: Document, useUpdatedList: Boolean): List<Manga> {
		val articles = if (useUpdatedList) {
			doc.select("article.item.movies")
		} else {
			doc.select("article.item").filter { article ->
				article.selectFirst("a[href*=/ler-online/]") != null
			}
		}
		val seen = HashSet<String>()
		return articles.mapNotNull { article ->
			val detailsUrl = article.selectFirst("a[href*=/ler-online/]")?.attrAsAbsoluteUrl("href")
				?: return@mapNotNull null
			if (!seen.add(detailsUrl)) return@mapNotNull null
			parseMangaFromArticle(article, detailsUrl)
		}
	}

	private fun parseMangaFromArticle(article: Element, detailsUrl: String): Manga? {
		val href = detailsUrl.toRelativeUrl(domain)
		val title = article.selectFirst("h3 a")?.text()?.trim()
			?: article.selectFirst(".data h3 a")?.text()?.trim()
			?: article.selectFirst("img")?.attr("alt")?.trim()
			?: return null
		if (title.isEmpty()) return null
		val cover = article.selectFirst(".poster img")?.let { img ->
			img.attr("data-src").ifEmpty { img.attr("src") }
		}
		val statusText = article.selectFirst(".type_status")?.text()?.lowercase().orEmpty()
		return Manga(
			id = generateUid(href),
			url = href,
			publicUrl = detailsUrl,
			title = title,
			coverUrl = cover,
			altTitles = emptySet(),
			rating = RATING_UNKNOWN,
			tags = emptySet(),
			description = null,
			state = when {
				"completo" in statusText -> MangaState.FINISHED
				"ativo" in statusText -> MangaState.ONGOING
				else -> null
			},
			authors = emptySet(),
			contentRating = ContentRating.ADULT,
			source = source,
		)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val statusText = doc.select(".data_tvshow .data_info").firstOrNull { el ->
			el.selectFirst("div")?.text()?.contains("Status", ignoreCase = true) == true
		}?.selectFirst("span")?.text()?.lowercase().orEmpty()
		return manga.copy(
			title = doc.selectFirst("h1")?.text()?.trim().orEmpty().ifEmpty { manga.title },
			coverUrl = doc.selectFirst(".poster img")?.let { img ->
				img.attr("data-src").ifEmpty { img.attr("src") }
			} ?: manga.coverUrl,
			description = doc.selectFirst(".sinopse .texto")?.html(),
			tags = doc.select(".gen_flex a[href*=/genero/]").mapNotNullToSet { a ->
				val key = a.attr("href").removeSuffix("/").substringAfterLast('/')
				if (key.isEmpty()) return@mapNotNullToSet null
				MangaTag(key = key, title = a.text().toTitleCase(), source = source)
			},
			state = when {
				"completo" in statusText -> MangaState.FINISHED
				"ativo" in statusText -> MangaState.ONGOING
				else -> manga.state
			},
			chapters = doc.select("ul.capitulos li.caps a").mapChapters(reversed = true) { i, a ->
				val href = a.attrAsRelativeUrl("href")
				val title = a.selectLast("span")?.text()?.trim() ?: a.text().trim()
				MangaChapter(
					id = generateUid(href),
					title = title,
					number = i + 1f,
					volume = 0,
					url = href,
					scanlator = null,
					uploadDate = 0,
					branch = null,
					source = source,
				)
			},
			contentRating = ContentRating.ADULT,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		return doc.select("img#nolzy").mapNotNull { img ->
			val url = img.attr("src").trim().ifEmpty { img.attr("data-src").trim() }
			if (url.isEmpty() || url.startsWith("data:")) return@mapNotNull null
			val relative = url.toRelativeUrl(domain)
			MangaPage(
				id = generateUid(relative),
				url = relative,
				preview = null,
				source = source,
			)
		}
	}
}
