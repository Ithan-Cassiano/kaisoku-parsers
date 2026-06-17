package com.kosen.reader.parsers.site.pt.nexustoons

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
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
import com.kosen.reader.parsers.model.MangaState
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.model.RATING_UNKNOWN
import com.kosen.reader.parsers.model.SortOrder
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.json.getStringOrNull
import com.kosen.reader.parsers.util.json.mapJSONNotNull
import com.kosen.reader.parsers.util.nullIfEmpty
import com.kosen.reader.parsers.util.parseJson
import com.kosen.reader.parsers.util.parseSafe
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.Locale

@MangaSourceParser("NEXUSTOONS", "NexusToons", "pt")
internal class NexusToons(context: MangaLoaderContext) : PagedMangaParser(
	context,
	source = MangaParserSource.NEXUSTOONS,
	pageSize = 24,
) {

	@Volatile
	private var cachedCategories: Set<MangaTag>? = null
	private val categoriesMutex = Mutex()

	override val configKeyDomain = ConfigKey.Domain("nexustoons.com")

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.ALPHABETICAL,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = MangaListFilterOptions(
		availableTags = getOrCreateCategories(),
		availableStates = EnumSet.of(
			MangaState.ONGOING,
			MangaState.FINISHED,
			MangaState.PAUSED,
			MangaState.ABANDONED,
		),
		availableContentTypes = EnumSet.of(
			ContentType.MANGA,
			ContentType.MANHWA,
			ContentType.MANHUA,
			ContentType.COMICS,
		),
		availableContentRating = EnumSet.of(
			ContentRating.SAFE,
			ContentRating.SUGGESTIVE,
			ContentRating.ADULT,
		),
	)

	override fun getRequestHeaders(): Headers = super.getRequestHeaders().newBuilder()
		.set("Accept", "application/json")
		.set("Referer", "https://$domain/")
		.set("Origin", "https://$domain")
		.build()

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val query = filter.query?.trim()?.nullIfEmpty()
		val url = apiUrl("/mangas").newBuilder()
			.addQueryParameter("page", page.toString())
			.addQueryParameter("limit", pageSize.toString())
			.addQueryParameter("sort", order.toApiSort())
			.apply {
				query?.let { addQueryParameter("search", it) }
				filter.tags.firstOrNull()?.key?.removePrefix(CATEGORY_PREFIX)?.let {
					addQueryParameter("categories", it)
				}
				stateToApi(filter.states.firstOrNull())?.let { addQueryParameter("status", it) }
				typeToApi(filter.types.firstOrNull())?.let { addQueryParameter("type", it) }
				when (filter.contentRating.singleOrNull()) {
					ContentRating.ADULT -> addQueryParameter("nsfw", "true")
					ContentRating.SAFE -> addQueryParameter("nsfw", "false")
					else -> Unit
				}
			}
			.build()

		val items = requestMangaArray(url.toString())
		return items.mapJSONNotNull { parseManga(it) }
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val slug = manga.url.removePrefix("/manga/").trim('/')
		val json = requestJson(apiUrl("/manga/$slug").toString())
		val title = json.getStringOrNull("title") ?: manga.title
		val description = json.getStringOrNull("description")?.nullIfEmpty()
		val cover = json.getStringOrNull("coverImage")?.nullIfEmpty()
		val altTitles = json.getStringOrNull("alternativeTitles")
			?.split(';')
			?.mapNotNullTo(mutableSetOf()) { it.trim().nullIfEmpty() }
			.orEmpty()
		val authors = setOfNotNull(
			json.getStringOrNull("author")?.nullIfEmpty(),
			json.getStringOrNull("artist")?.nullIfEmpty(),
		)
		val tags = json.optJSONArray("categories")?.mapJSONNotNull { category ->
			val name = category.getStringOrNull("name") ?: return@mapJSONNotNull null
			MangaTag(
				key = category.optString("slug", name.lowercase(Locale.ROOT)),
				title = name,
				source = source,
			)
		}?.toSet().orEmpty()
		val chapters = json.optJSONArray("chapters")?.mapJSONNotNull { chapterJson ->
			parseChapter(chapterJson)
		}.orEmpty().sortedBy { it.number }
		return manga.copy(
			title = title,
			description = description ?: manga.description,
			coverUrl = cover ?: manga.coverUrl,
			largeCoverUrl = cover ?: manga.largeCoverUrl,
			altTitles = altTitles,
			authors = authors,
			tags = tags,
			state = parseState(json.getStringOrNull("status")),
			contentRating = resolveContentRating(json),
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterId = chapter.url.removePrefix("/chapter/").substringBefore('/')
		val json = requestJson(apiUrl("/read/$chapterId").toString())
		val pageToken = json.getStringOrNull("pageToken")
			?: throw Exception("Capítulo indisponível")
		val pages = json.optJSONArray("pages") ?: JSONArray()
		return List(pages.length()) { index ->
			MangaPage(
				id = generateUid("$pageToken/$index"),
				url = apiUrl("/p/$pageToken/$index").toString(),
				preview = null,
				source = source,
			)
		}
	}

	private suspend fun getOrCreateCategories(): Set<MangaTag> = categoriesMutex.withLock {
		cachedCategories?.let { return it }
		val tags = runCatching {
			requestJsonArray(apiUrl("/categories").toString()).mapJSONNotNull { category ->
				val id = category.optInt("id", 0)
				val name = category.getStringOrNull("name") ?: return@mapJSONNotNull null
				if (id <= 0) return@mapJSONNotNull null
				MangaTag(
					key = "$CATEGORY_PREFIX$id",
					title = name,
					source = source,
				)
			}.toSet()
		}.getOrDefault(emptySet())
		cachedCategories = tags
		tags
	}

	private suspend fun requestJson(url: String): JSONObject {
		val raw = webClient.httpGet(url, getRequestHeaders()).parseJson()
		return NexusToonsCrypto.decryptResponse(raw)
	}

	private suspend fun requestJsonArray(url: String): JSONArray {
		val raw = webClient.httpGet(url, getRequestHeaders()).use { response ->
			response.body.string().trim()
		}
		return when {
			raw.startsWith("[") -> JSONArray(raw)
			else -> {
				val json = JSONObject(raw)
				if (NexusToonsCrypto.isEncryptedResponse(json)) {
					JSONArray(NexusToonsCrypto.decryptPayload(json))
				} else {
					json.optJSONArray("data") ?: JSONArray()
				}
			}
		}
	}

	private suspend fun requestMangaArray(url: String): JSONArray {
		val raw = webClient.httpGet(url, getRequestHeaders()).use { response ->
			response.body.string().trim()
		}
		if (raw.startsWith("[")) {
			return JSONArray(raw)
		}
		val json = JSONObject(raw)
		val decrypted = NexusToonsCrypto.decryptResponse(json)
		return decrypted.optJSONArray("data") ?: JSONArray()
	}

	private fun apiUrl(path: String) = "https://$domain/api$path".toHttpUrl()

	private fun stateToApi(state: MangaState?): String? = when (state) {
		MangaState.ONGOING -> "ongoing"
		MangaState.FINISHED -> "completed"
		MangaState.PAUSED -> "hiatus"
		MangaState.ABANDONED -> "cancelled"
		else -> null
	}

	private fun SortOrder.toApiSort(): String = when (this) {
		SortOrder.POPULARITY -> "views"
		SortOrder.UPDATED -> "updatedAt"
		SortOrder.ALPHABETICAL -> "title"
		else -> "updatedAt"
	}

	private fun typeToApi(type: ContentType?): String? = when (type) {
		ContentType.MANGA -> "manga"
		ContentType.MANHWA -> "manhwa"
		ContentType.MANHUA -> "manhua"
		ContentType.COMICS -> "webtoon"
		else -> null
	}

	private fun parseManga(json: JSONObject): Manga? {
		val slug = json.getStringOrNull("slug") ?: return null
		val id = json.optInt("id", 0)
		val href = "/manga/$slug"
		return Manga(
			id = generateUid(id.takeIf { it > 0 }?.toString() ?: slug),
			title = json.getStringOrNull("title").orEmpty(),
			altTitles = emptySet(),
			url = href,
			publicUrl = "https://$domain/manga/$slug",
			rating = RATING_UNKNOWN,
			contentRating = resolveContentRating(json),
			coverUrl = json.getStringOrNull("coverImage")?.nullIfEmpty(),
			tags = emptySet(),
			state = parseState(json.getStringOrNull("status")),
			authors = emptySet(),
			largeCoverUrl = null,
			description = null,
			source = source,
		)
	}

	private fun parseChapter(json: JSONObject): MangaChapter? {
		if (json.optString("accessLevel") != "public") {
			return null
		}
		if (json.optInt("coinCost", 0) > 0) {
			return null
		}
		val chapterId = json.optInt("id", 0)
		if (chapterId <= 0) {
			return null
		}
		val number = json.getStringOrNull("number")?.replace(',', '.')?.toFloatOrNull() ?: 0f
		val title = json.getStringOrNull("title")?.nullIfEmpty()
		return MangaChapter(
			id = generateUid(chapterId.toLong()),
			title = title ?: "Capítulo ${json.getStringOrNull("number").orEmpty()}",
			number = number,
			volume = 0,
			url = "/chapter/$chapterId",
			scanlator = null,
			uploadDate = chapterDateFormat.parseSafe(json.getStringOrNull("createdAt")),
			branch = null,
			source = source,
		)
	}

	private fun parseState(raw: String?): MangaState? = when (raw?.trim()?.lowercase(Locale.ROOT)) {
		"ongoing", "em andamento" -> MangaState.ONGOING
		"completed", "completo", "finished", "finalizado" -> MangaState.FINISHED
		"hiatus", "hiato", "paused", "pausado" -> MangaState.PAUSED
		"cancelled", "canceled", "cancelado" -> MangaState.ABANDONED
		else -> null
	}

	private fun resolveContentRating(json: JSONObject): ContentRating {
		if (json.optBoolean("isNsfw")) {
			return ContentRating.ADULT
		}
		if (json.optBoolean("isSuggestive")) {
			return ContentRating.SUGGESTIVE
		}
		val categories = json.optJSONArray("categories") ?: return ContentRating.SAFE
		for (index in 0 until categories.length()) {
			val category = categories.optJSONObject(index) ?: continue
			val nested = category.optJSONObject("category") ?: category
			if (nested.optBoolean("isNsfw")) {
				return ContentRating.ADULT
			}
		}
		return ContentRating.SAFE
	}

	private companion object {
		private const val CATEGORY_PREFIX = "cat:"
		private val chapterDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)
	}
}
