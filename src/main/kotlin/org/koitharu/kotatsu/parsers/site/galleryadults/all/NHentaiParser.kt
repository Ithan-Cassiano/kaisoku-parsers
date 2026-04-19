package org.koitharu.kotatsu.parsers.site.galleryadults.all

import org.json.JSONObject
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.site.galleryadults.GalleryAdultsParser
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.*
import java.util.*

@MangaSourceParser("NHENTAI", "NHentai.net", type = ContentType.HENTAI)
internal class NHentaiParser(context: MangaLoaderContext) :
	GalleryAdultsParser(context, MangaParserSource.NHENTAI, "nhentai.net", 25) {

	override val availableSortOrders: Set<SortOrder> =
		EnumSet.of(SortOrder.UPDATED, SortOrder.POPULARITY, SortOrder.POPULARITY_TODAY, SortOrder.POPULARITY_WEEK)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = super.filterCapabilities.copy(
			isMultipleTagsSupported = true,
		)

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = emptySet(),
		availableLocales = setOf(Locale.ENGLISH, Locale.JAPANESE, Locale.CHINESE),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val numericQuery = filter.query?.trim()?.takeIf { it.matches("\\d+".toRegex()) }
		if (numericQuery != null) {
			return runCatching {
				listOf(parseGallery(fetchGallery(numericQuery.toLong())))
			}.getOrDefault(emptyList())
		}

		val query = buildString {
			append("pages:>0")
			when {
				!filter.query.isNullOrEmpty() -> {
					append(' ')
					append(filter.query)
				}

				else -> {
					val filters = buildQuery(filter.tags, filter.locale)
					if (filters.isNotEmpty()) {
						append(' ')
						append(filters)
					}
				}
			}
		}
		val url = buildString {
			append("https://")
			append(domain)
			append("/api/v2/search?query=")
			append(query.urlEncoded())
			append("&page=")
			append(page.coerceAtLeast(1).toString())
			append("&sort=")
			append(
				when (order) {
					SortOrder.POPULARITY -> "popular"
					SortOrder.POPULARITY_TODAY -> "popular-today"
					SortOrder.POPULARITY_WEEK -> "popular-week"
					else -> "date"
				},
			)
		}
		return webClient.httpGet(url).parseJson()
			.getJSONArray("result")
			.mapJSON(::parseSearchItem)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		return parseGallery(fetchGallery(galleryId(manga.url)), manga)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val gallery = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseJson()
		return gallery.getJSONArray("pages").mapJSON { page ->
			val url = imageUrl(page.getString("path"))
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = page.getStringOrNull("thumbnail")?.let(::thumbnailUrl),
				source = source,
			)
		}
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url

	private fun buildQuery(tags: Collection<MangaTag>, language: Locale?): String {
		val filters = ArrayList<String>(tags.size + 1)
		tags.forEach { tag ->
			filters += "tag:\"${tag.key}\""
		}
		language?.let { lc ->
			filters += "language:\"${lc.toLanguagePath()}\""
		}
		return filters.joinToString(separator = " ")
	}

	private suspend fun fetchGallery(id: Long): JSONObject {
		return webClient.httpGet("https://$domain/api/v2/galleries/$id").parseJson()
	}

	private fun parseSearchItem(json: JSONObject): Manga {
		val id = json.getLong("id")
		val href = "/g/$id/"
		return Manga(
			id = generateUid(href),
			title = json.getStringOrNull("english_title")
				?: json.getStringOrNull("japanese_title")
				?: id.toString(),
			altTitles = setOfNotNull(json.getStringOrNull("japanese_title")),
			url = href,
			publicUrl = href.toAbsoluteUrl(domain),
			rating = RATING_UNKNOWN,
			contentRating = if (isNsfwSource) ContentRating.ADULT else null,
			coverUrl = thumbnailUrl(json.getString("thumbnail")),
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			source = source,
		)
	}

	private fun parseGallery(json: JSONObject, base: Manga? = null): Manga {
		val id = json.getLong("id")
		val href = "/g/$id/"
		val titleObj = json.getJSONObject("title")
		val title = titleObj.getStringOrNull("english")
			?: titleObj.getStringOrNull("pretty")
			?: titleObj.getStringOrNull("japanese")
			?: base?.title
			?: id.toString()
		val tagsJson = json.getJSONArray("tags")
		val pagesUrl = "/api/v2/galleries/$id"
		val sourceManga = base ?: Manga(
			id = generateUid(href),
			title = title,
			altTitles = emptySet(),
			url = href,
			publicUrl = href.toAbsoluteUrl(domain),
			rating = RATING_UNKNOWN,
			contentRating = if (isNsfwSource) ContentRating.ADULT else null,
			coverUrl = json.optJSONObject("thumbnail")?.getStringOrNull("path")?.let(::thumbnailUrl),
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			source = source,
		)
		return sourceManga.copy(
			id = generateUid(href),
			title = title.cleanupTitle(),
			altTitles = setOfNotNull(
				titleObj.getStringOrNull("japanese")?.cleanupTitle(),
				titleObj.getStringOrNull("pretty")?.cleanupTitle(),
			).filterNotTo(LinkedHashSet()) { it == title },
			url = href,
			publicUrl = href.toAbsoluteUrl(domain),
			coverUrl = json.optJSONObject("thumbnail")?.getStringOrNull("path")?.let(::thumbnailUrl),
			largeCoverUrl = json.optJSONObject("cover")?.getStringOrNull("path")?.let(::thumbnailUrl),
			tags = tagsJson.mapJSONNotNullToSet { tag ->
				val type = tag.getStringOrNull("type")
				if (type != "tag" && type != "category") {
					return@mapJSONNotNullToSet null
				}
				MangaTag(
					key = tag.getString("slug"),
					title = tag.getString("name").toTitleCase(sourceLocale),
					source = source,
				)
			},
			authors = tagsJson.mapJSONNotNullToSet { tag ->
				tag.takeIf { it.getStringOrNull("type") == "artist" }?.getStringOrNull("name")
			},
			chapters = listOf(
				MangaChapter(
					id = generateUid(pagesUrl),
					title = title,
					number = 1f,
					volume = 0,
					url = pagesUrl,
					scanlator = json.getStringOrNull("scanlator"),
					uploadDate = json.getLongOrDefault("upload_date", 0L) * 1000L,
					branch = tagsJson.mapJSONNotNull { tag ->
						val type = tag.getStringOrNull("type")
						val slug = tag.getStringOrNull("slug")
						tag.takeIf { type == "language" && slug != "translated" }?.getStringOrNull("name")
					}.joinToString(separator = " / ").ifEmpty { null },
					source = source,
				),
			),
		)
	}

	private fun galleryId(url: String): Long {
		return url.removeSuffix("/").substringAfterLast('/').toLong()
	}

	private fun thumbnailUrl(path: String): String = "https://t.nhentai.net/${path.removePrefix("/")}"

	private fun imageUrl(path: String): String = "https://i.nhentai.net/${path.removePrefix("/")}"
}
