package com.kosen.reader.parsers.site.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.core.PagedMangaParser
import com.kosen.reader.parsers.model.ContentRating
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaListFilter
import com.kosen.reader.parsers.model.MangaListFilterCapabilities
import com.kosen.reader.parsers.model.MangaListFilterOptions
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.model.RATING_UNKNOWN
import com.kosen.reader.parsers.model.SortOrder
import com.kosen.reader.parsers.util.attrAsAbsoluteUrl
import com.kosen.reader.parsers.util.attrAsRelativeUrl
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.mapChapters
import com.kosen.reader.parsers.util.mapToSet
import com.kosen.reader.parsers.util.oneOrThrowIfMany
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.src
import com.kosen.reader.parsers.util.toAbsoluteUrl
import com.kosen.reader.parsers.util.toTitleCase
import com.kosen.reader.parsers.util.urlEncoded
import java.util.EnumSet

@MangaSourceParser("INSTAHENTAI", "InstaHentai", "pt", ContentType.HENTAI)
internal class InstaHentai(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.INSTAHENTAI, pageSize = 18) {

	override val configKeyDomain = ConfigKey.Domain("instahentai.com")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(SortOrder.POPULARITY)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchTags(),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			if (!filter.query.isNullOrEmpty()) {
				append("/page/")
				append(page)
				append("/?s=")
				append(filter.query.urlEncoded())
			} else {
				val tagSlug = filter.tags.oneOrThrowIfMany()?.key
				if (tagSlug != null) {
					append("/generos/")
					append(tagSlug)
					append('/')
				} else {
					append("/melhores/")
				}
				append("page/")
				append(page)
				append('/')
			}
		}
		val doc = webClient.httpGet(url).parseHtml()
		return doc.select("article.card_item").mapNotNull { card ->
			val link = card.selectFirst("a[href*=/serie/]") ?: return@mapNotNull null
			val href = link.attrAsRelativeUrl("href")
			val title = card.selectFirst(".serie_title h3 a")?.text()?.trim()
				?: link.attr("aria-label").trim()
			Manga(
				id = generateUid(href),
				title = title,
				altTitles = emptySet(),
				url = href,
				publicUrl = href.toAbsoluteUrl(domain),
				rating = RATING_UNKNOWN,
				contentRating = ContentRating.ADULT,
				coverUrl = card.selectFirst("img")?.src(),
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				source = source,
			)
		}
	}

	private suspend fun fetchTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain/hentai-generos/").parseHtml()
		return buildSet {
			doc.select("a[href*=/generos/]").forEach { a ->
				val key = a.attr("href").trimEnd('/').substringAfterLast('/')
				val title = a.text().trim()
				if (key.isNotEmpty() && key != "generos" && title.isNotEmpty()) {
					add(
						MangaTag(
							key = key,
							title = title.toTitleCase(sourceLocale),
							source = source,
						),
					)
				}
			}
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val authors = HashSet<String>()
		doc.select("a[href*=/autor/][rel=tag], a[href*=/arte/][rel=tag]").forEach { a ->
			val name = a.text().trim()
			if (name.isNotEmpty()) authors.add(name)
		}
		val tags = doc.select("a[href*=/generos/][rel=tag]").mapToSet { a ->
			MangaTag(
				key = a.attr("href").trimEnd('/').substringAfterLast('/'),
				title = a.text().trim().toTitleCase(sourceLocale),
				source = source,
			)
		}
		val description = doc.selectFirst("#textDiv")?.html()
		val chapters = doc.select("#section_cap > a").mapChapters { i, a ->
			val href = a.attrAsRelativeUrl("href")
			MangaChapter(
				id = generateUid(href),
				title = a.text().trim(),
				number = (i + 1).toFloat(),
				volume = 0,
				url = href,
				scanlator = null,
				uploadDate = 0L,
				branch = null,
				source = source,
			)
		}
		return manga.copy(
			authors = authors,
			tags = tags,
			description = description,
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		return doc.select("#Imagens noscript img").mapNotNull { img ->
			val url = img.attrAsAbsoluteUrl("src").ifEmpty { return@mapNotNull null }
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}
}
