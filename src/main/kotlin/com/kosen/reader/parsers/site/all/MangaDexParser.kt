package com.kosen.reader.parsers.site.all

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.core.FlexibleMangaParser
import com.kosen.reader.parsers.exception.ParseException
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.model.search.MangaSearchQuery
import com.kosen.reader.parsers.model.search.MangaSearchQueryCapabilities
import com.kosen.reader.parsers.model.search.QueryCriteria.*
import com.kosen.reader.parsers.model.search.SearchCapability
import com.kosen.reader.parsers.model.search.SearchableField
import com.kosen.reader.parsers.model.search.SearchableField.*
import com.kosen.reader.parsers.util.*
import com.kosen.reader.parsers.util.json.*
import java.text.SimpleDateFormat
import java.util.*

private const val PAGE_SIZE = 20
private const val CHAPTERS_FIRST_PAGE_SIZE = 120
private const val CHAPTERS_MAX_PAGE_SIZE = 500
private const val CHAPTERS_PARALLELISM = 3
private const val CHAPTERS_MAX_COUNT = 10_000 // strange api behavior, looks like a bug
private const val LOCALE_FALLBACK = "en"
private const val SERVER_DATA = "data"
private const val SERVER_DATA_SAVER = "data-saver"
private const val PRESET_ADULT = "kosen:preset/adult"
private const val PRESET_ADULT_MANHWA = "kosen:preset/adult-manhwa"
private const val PRESET_ADULT_MANHUA = "kosen:preset/adult-manhua"

private val QUICK_FILTER_TAGS = listOf(
	PRESET_ADULT to "+18",
	PRESET_ADULT_MANHWA to "+18 Manhwa",
	PRESET_ADULT_MANHUA to "+18 Manhua",
)

@MangaSourceParser("MANGADEX", "MangaDex")
internal class MangaDexParser(context: MangaLoaderContext) : FlexibleMangaParser(context, MangaParserSource.MANGADEX) {

	override val configKeyDomain = ConfigKey.Domain("mangadex.org")

	private val preferredServerKey = ConfigKey.PreferredImageServer(
		presetValues = mapOf(
			SERVER_DATA to "Original quality",
			SERVER_DATA_SAVER to "Compressed quality",
		),
		defaultValue = SERVER_DATA,
	)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(preferredServerKey)
	}

	override val availableSortOrders: EnumSet<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.UPDATED_ASC,
		SortOrder.POPULARITY,
		SortOrder.POPULARITY_ASC,
		SortOrder.RATING,
		SortOrder.RATING_ASC,
		SortOrder.NEWEST,
		SortOrder.NEWEST_ASC,
		SortOrder.ALPHABETICAL,
		SortOrder.ALPHABETICAL_DESC,
		SortOrder.ADDED,
		SortOrder.ADDED_ASC,
		SortOrder.RELEVANCE,
	)

	override val searchQueryCapabilities: MangaSearchQueryCapabilities
		get() = MangaSearchQueryCapabilities(
			SearchCapability(
				field = TAG,
				criteriaTypes = setOf(Include::class, Exclude::class),
				isMultiple = true,
			),
			SearchCapability(
				field = TITLE_NAME,
				criteriaTypes = setOf(Match::class),
				isMultiple = false,
			),
			SearchCapability(
				field = STATE,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = AUTHOR,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = CONTENT_TYPE,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = CONTENT_RATING,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = DEMOGRAPHIC,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = ORIGINAL_LANGUAGE,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = LANGUAGE,
				criteriaTypes = setOf(Include::class),
				isMultiple = true,
			),
			SearchCapability(
				field = PUBLICATION_YEAR,
				criteriaTypes = setOf(Match::class),
				isMultiple = false,
			),
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = coroutineScope {
		val localesDeferred = async { fetchAvailableLocales() }
		val tagsDeferred = async { fetchAvailableTags() }
		val quickFilters = QUICK_FILTER_TAGS.map { (key, title) ->
			MangaTag(title = title, key = key, source = source)
		}.toSet()
		MangaListFilterOptions(
			availableTags = quickFilters + tagsDeferred.await(),
			availableStates = EnumSet.of(
				MangaState.ONGOING,
				MangaState.FINISHED,
				MangaState.PAUSED,
				MangaState.ABANDONED,
			),
			availableContentRating = EnumSet.of(
				ContentRating.SAFE,
				ContentRating.SUGGESTIVE,
				ContentRating.ADULT,
			),
			availableContentTypes = EnumSet.of(
				ContentType.MANGA,
				ContentType.MANHWA,
				ContentType.MANHUA,
				ContentType.DOUJINSHI,
			),
			availableDemographics = EnumSet.of(
				Demographic.SHOUNEN,
				Demographic.SHOUJO,
				Demographic.SEINEN,
				Demographic.JOSEI,
				Demographic.NONE,
			),
			availableLocales = localesDeferred.await(),
		)
	}

	private fun SearchableField.toParamName(): String = when (this) {
		TITLE_NAME -> "title"
		TAG -> "includedTags[]"
		AUTHOR -> "authors[]"
		STATE -> "status[]"
		CONTENT_TYPE -> "contentType[]"
		CONTENT_RATING -> "contentRating[]"
		DEMOGRAPHIC -> "publicationDemographic[]"
		ORIGINAL_LANGUAGE -> "originalLanguage[]"
		LANGUAGE -> "availableTranslatedLanguage[]"
		PUBLICATION_YEAR -> "year"
	}

	private fun Any?.toQueryParam(): String = when (this) {
		is String -> urlEncoded()
		is Locale -> if (language == "in") "id" else language
		is MangaTag -> key
		is MangaState -> when (this) {
			MangaState.ONGOING -> "ongoing"
			MangaState.FINISHED -> "completed"
			MangaState.ABANDONED -> "cancelled"
			MangaState.PAUSED -> "hiatus"
			else -> ""
		}

		is ContentRating -> when (this) {
			ContentRating.SAFE -> "safe"
			ContentRating.SUGGESTIVE -> "suggestive"
			ContentRating.ADULT -> "pornographic"
		}

		is ContentType -> when (this) {
			ContentType.MANGA -> "ja"
			ContentType.MANHWA -> "ko"
			ContentType.MANHUA -> "zh"
			ContentType.DOUJINSHI -> "ja"
			else -> ""
		}

		is Demographic -> when (this) {
			Demographic.SHOUNEN -> "shounen"
			Demographic.SHOUJO -> "shoujo"
			Demographic.SEINEN -> "seinen"
			Demographic.JOSEI -> "josei"
			Demographic.NONE -> "none"
			else -> ""
		}

		is SortOrder -> when (this) {
			SortOrder.UPDATED -> "[latestUploadedChapter]=desc"
			SortOrder.UPDATED_ASC -> "[latestUploadedChapter]=asc"
			SortOrder.RATING -> "[rating]=desc"
			SortOrder.RATING_ASC -> "[rating]=asc"
			SortOrder.ALPHABETICAL -> "[title]=asc"
			SortOrder.ALPHABETICAL_DESC -> "[title]=desc"
			SortOrder.NEWEST -> "[year]=desc"
			SortOrder.NEWEST_ASC -> "[year]=asc"
			SortOrder.POPULARITY -> "[followedCount]=desc"
			SortOrder.POPULARITY_ASC -> "[followedCount]=asc"
			SortOrder.ADDED -> "[createdAt]=desc"
			SortOrder.ADDED_ASC -> "[createdAt]=asc"
			SortOrder.RELEVANCE -> "[relevance]=desc"
			else -> "[latestUploadedChapter]=desc"
		}

		else -> this.toString().urlEncoded()
	}

	private fun StringBuilder.appendCriterion(field: SearchableField, value: Any?, paramName: String? = null) {
		val param = paramName ?: field.toParamName()
		if (param.isNotBlank()) {
			append("&$param=")
			append(value.toQueryParam())
		}
	}

	private fun StringBuilder.appendContentRating(value: ContentRating) {
		when (value) {
			ContentRating.SAFE -> append("&contentRating[]=safe")
			ContentRating.SUGGESTIVE -> append("&contentRating[]=suggestive")
			ContentRating.ADULT -> {
				append("&contentRating[]=erotica")
				append("&contentRating[]=pornographic")
			}
		}
	}

	private fun StringBuilder.appendContentType(value: ContentType) {
		val language = value.toQueryParam().takeIf { it.isNotEmpty() } ?: return
		append("&originalLanguage[]=")
		append(language)
	}

	private fun StringBuilder.appendPresetTag(key: String): Boolean = when (key) {
		PRESET_ADULT -> {
			appendContentRating(ContentRating.ADULT)
			true
		}
		PRESET_ADULT_MANHWA -> {
			append("&originalLanguage[]=ko")
			appendContentRating(ContentRating.ADULT)
			true
		}
		PRESET_ADULT_MANHUA -> {
			append("&originalLanguage[]=zh")
			appendContentRating(ContentRating.ADULT)
			true
		}
		else -> false
	}

	private fun StringBuilder.appendIncludeCriterion(criterion: Include<*>, onContentRating: () -> Unit) {
		when (criterion.field) {
			CONTENT_RATING -> {
				onContentRating()
				criterion.values.filterIsInstance<ContentRating>().forEach { appendContentRating(it) }
			}
			CONTENT_TYPE -> {
				criterion.values.filterIsInstance<ContentType>().forEach { appendContentType(it) }
			}
			TAG -> {
				criterion.values.filterIsInstance<MangaTag>().forEach { tag ->
					if (!appendPresetTag(tag.key)) {
						appendCriterion(TAG, tag)
					} else {
						onContentRating()
					}
				}
			}
			else -> criterion.values.forEach { appendCriterion(criterion.field, it) }
		}
	}

	override suspend fun getList(query: MangaSearchQuery): List<Manga> {
		val url = buildString {
			append("https://api.$domain/manga?limit=$PAGE_SIZE&offset=${query.offset}")
				.append("&includes[]=cover_art&includes[]=author&includes[]=artist&includedTagsMode=AND&excludedTagsMode=OR")

			var hasContentRating = false
			val markContentRating = { hasContentRating = true }

			query.criteria.forEach { criterion ->
				when (criterion) {
					is Include<*> -> appendIncludeCriterion(criterion, markContentRating)

					is Exclude<*> -> {
						criterion.values.filterIsInstance<MangaTag>()
							.filter { tag -> QUICK_FILTER_TAGS.none { it.first == tag.key } }
							.forEach { appendCriterion(TAG, it, "excludedTags[]") }
					}

					is Match<*> -> {
						appendCriterion(criterion.field, criterion.value)
					}

					else -> {
						// Not supported
					}
				}
			}

			if (!hasContentRating) {
				append("&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&contentRating[]=pornographic")
			}

			append("&order")
			append((query.order ?: defaultSortOrder).toQueryParam())
		}

		val json = webClient.httpGet(url).parseJson().getJSONArray("data")
		return json.mapJSON { jo -> jo.fetchManga(null) }
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val mangaId = manga.url.removePrefix("/")
		return getDetails(mangaId)
	}

	override suspend fun resolveLink(resolver: LinkResolver, link: HttpUrl): Manga? {
		val regex = Regex("[0-9a-f\\-]{10,}", RegexOption.IGNORE_CASE)
		val mangaId = link.pathSegments.find { regex.matches(it) } ?: return null
		return getDetails(mangaId)
	}

	private suspend fun getDetails(mangaId: String): Manga = coroutineScope {
		val jsonDeferred = async {
			webClient.httpGet(
				"https://api.$domain/manga/${mangaId}?includes[]=artist&includes[]=author&includes[]=cover_art",
			).parseJson().getJSONObject("data")
		}
		val feedDeferred = async { loadChapters(mangaId) }
		jsonDeferred.await().fetchManga(mapChapters(feedDeferred.await()))
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val json = webClient.httpGet(
			"https://api.$domain/at-home/server/${chapter.url}?forcePort443=false",
		).parseJson()
		val chapterJson = json.getJSONObject("chapter")
		val server = config[preferredServerKey] ?: SERVER_DATA
		val pages = chapterJson.getJSONArray(
			if (server == SERVER_DATA_SAVER) "dataSaver" else "data",
		)
		val prefix = "${json.getString("baseUrl")}/$server/${chapterJson.getString("hash")}/"
		val dataSaverPages = chapterJson.optJSONArray("dataSaver")
		val dataSaverPrefix = "${json.getString("baseUrl")}/$SERVER_DATA_SAVER/${chapterJson.getString("hash")}/"
		return List(pages.length()) { i ->
			val url = prefix + pages.getString(i)
			val previewUrl = if (server == SERVER_DATA_SAVER) {
				null
			} else {
				dataSaverPages?.optString(i)?.nullIfEmpty()?.let { dataSaverPrefix + it }
			}
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = previewUrl,
				source = source,
			)
		}
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		val tags = webClient.httpGet("https://api.${domain}/manga/tag").parseJson()
			.getJSONArray("data")
		return tags.mapJSONToSet { jo ->
			MangaTag(
				title = jo.getJSONObject("attributes").getJSONObject("name")
					.firstStringValue()
					.toTitleCase(Locale.ENGLISH),
				key = jo.getString("id"),
				source = source,
			)
		}
	}

	private suspend fun fetchAvailableLocales(): Set<Locale> {
		val fromSite = runCatching {
			val head = webClient.httpGet("https://$domain/").parseHtml().head()
			head.getElementsByAttributeValue("property", "og:locale:alternate")
				.mapNotNullToSet { meta ->
					val raw = meta.attrOrNull("content") ?: return@mapNotNullToSet null
					Locale(raw.substringBefore('_'), raw.substringAfter('_', ""))
				}
		}.getOrDefault(emptySet())
		if (fromSite.isNotEmpty()) {
			return fromSite
		}
		return setOf(
			Locale.ENGLISH,
			Locale.JAPANESE,
			Locale.KOREAN,
			Locale.CHINESE,
			Locale("pt", "BR"),
			Locale("es"),
		)
	}

	private fun JSONObject.fetchManga(chapters: List<MangaChapter>?): Manga {
		val id = getString("id")
		val attrs = getJSONObject("attributes")
		val relations = getJSONArray("relationships").associateByKey("type")
		val cover = relations["cover_art"]
			?.firstOrNull()
			?.getJSONObject("attributes")
			?.getString("fileName")
			?.let {
				"https://uploads.$domain/covers/$id/$it"
			}
		val authors: Set<String> = (relations["author"] ?: relations["artist"])
			?.mapNotNullToSet {
				it.getJSONObject("attributes")?.getStringOrNull("name")
			}.orEmpty()

		return Manga(
			id = generateUid(id),
			title = requireNotNull(attrs.getJSONObject("title").selectByLocale()) {
				"Title should not be null"
			},
			altTitles = attrs.optJSONArray("altTitles")?.selectAltTitlesByLocale().orEmpty(),
			url = id,
			publicUrl = "https://$domain/title/$id",
			rating = RATING_UNKNOWN,
			contentRating = when (attrs.getStringOrNull("contentRating")) {
				"pornographic" -> ContentRating.ADULT
				"erotica", "suggestive" -> ContentRating.SUGGESTIVE
				"safe" -> ContentRating.SAFE
				else -> null
			},
			coverUrl = cover?.plus(".256.jpg"),
			largeCoverUrl = cover,
			description = attrs.optJSONObject("description")?.selectByLocale(),
			tags = attrs.getJSONArray("tags").mapJSONToSet { tag ->
				MangaTag(
					title = tag.getJSONObject("attributes")
						.getJSONObject("name")
						.firstStringValue()
						.toTitleCase(),
					key = tag.getString("id"),
					source = source,
				)
			},
			state = when (attrs.getStringOrNull("status")) {
				"ongoing" -> MangaState.ONGOING
				"completed" -> MangaState.FINISHED
				"hiatus" -> MangaState.PAUSED
				"cancelled" -> MangaState.ABANDONED
				else -> null
			},
			authors = authors,
			chapters = chapters,
			source = source,
		)
	}

	private fun JSONObject.firstStringValue() = entries<String>().first().value

	private fun JSONObject.selectByLocale(): String? {
		val preferredLocales = context.getPreferredLocales()
		for (locale in preferredLocales) {
			getStringOrNull(locale.language)?.let { return it }
			getStringOrNull(locale.toLanguageTag())?.let { return it }
		}
		return getStringOrNull(LOCALE_FALLBACK) ?: entries<String>().firstOrNull()?.value?.nullIfEmpty()
	}

	// altTitles is an array of single-entry {locale: title} objects; only keep entries whose
	// locale matches the user's preferred locales (or the fallback locale) instead of every locale.
	private fun JSONArray.selectAltTitlesByLocale(): Set<String> {
		val allowedLocales = buildSet {
			context.getPreferredLocales().forEach { locale ->
				add(locale.language)
				add(locale.toLanguageTag())
			}
			add(LOCALE_FALLBACK)
		}
		val result = LinkedHashSet<String>()
		repeat(length()) { i ->
			val jo = optJSONObject(i) ?: return@repeat
			for (key in jo.keys()) {
				if (key !in allowedLocales) continue
				jo.getStringOrNull(key)?.nullIfEmpty()?.let { result.add(it) }
			}
		}
		return result
	}

	private suspend fun loadChapters(mangaId: String): List<JSONObject> {
		val firstPage = loadChapters(mangaId, offset = 0, limit = CHAPTERS_FIRST_PAGE_SIZE)
		if (firstPage.size >= firstPage.total) {
			return firstPage.data
		}
		val tail = coroutineScope {
			val leftCount = firstPage.total.coerceAtMost(CHAPTERS_MAX_COUNT) - firstPage.size
			val pages = (leftCount / CHAPTERS_MAX_PAGE_SIZE.toFloat()).toIntUp()
			val dispatcher = Dispatchers.Default.limitedParallelism(CHAPTERS_PARALLELISM)
			List(pages) { page ->
				val offset = page * CHAPTERS_MAX_PAGE_SIZE + firstPage.size
				async(dispatcher) {
					loadChapters(mangaId, offset, CHAPTERS_MAX_PAGE_SIZE)
				}
			}.awaitAll()
		}
		val result = ArrayList<JSONObject>(firstPage.total)
		result += firstPage.data
		tail.flatMapTo(result) { it.data }
		return result
	}

	private suspend fun loadChapters(mangaId: String, offset: Int, limit: Int): Chapters {
		val limitedLimit = when {
			offset >= CHAPTERS_MAX_COUNT -> return Chapters(emptyList(), CHAPTERS_MAX_COUNT)
			offset + limit > CHAPTERS_MAX_COUNT -> CHAPTERS_MAX_COUNT - offset
			else -> limit
		}
		val url = buildString {
			append("https://api.")
			append(domain)
			append("/manga/")
			append(mangaId)
			append("/feed")
			append("?limit=")
			append(limitedLimit)
			append("&includes[]=scanlation_group&order[volume]=asc&order[chapter]=asc&offset=")
			append(offset)
			append("&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&contentRating[]=pornographic")
		}
		val json = webClient.httpGet(url).parseJson()
		if (json.getString("result") == "ok") {
			return Chapters(
				data = json.optJSONArray("data")?.asTypedList<JSONObject>().orEmpty(),
				total = json.getInt("total"),
			)
		} else {
			val error = json.optJSONArray("errors").mapJSON { jo ->
				jo.getString("detail")
			}.joinToString("\n")
			throw ParseException(error, url)
		}
	}

	private fun mapChapters(list: List<JSONObject>): List<MangaChapter> {
		// 2022-01-02T00:27:11+00:00
		val dateFormat = SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'+00:00'",
			Locale.ROOT,
		)
		val chaptersBuilder = ChaptersListBuilder(list.size)
		val branchedChapters = HashMap<String?, HashMap<Pair<Int, Float>, MangaChapter>>()
		for (jo in list) {
			val id = jo.getString("id")
			val attrs = jo.getJSONObject("attributes")
			if (!attrs.isNull("externalUrl")) {
				continue
			}
			val number = attrs.getFloatOrDefault("chapter", 0f)
			val volume = attrs.getIntOrDefault("volume", 0)
			val locale = attrs.getStringOrNull("translatedLanguage")?.let { Locale.forLanguageTag(it) }
			val lc = locale?.getDisplayName(locale)?.toTitleCase(locale)
			val relations = jo.getJSONArray("relationships").associateByKey("type")
			val team =
				relations["scanlation_group"]?.firstOrNull()?.optJSONObject("attributes")?.getStringOrNull("name")
			val branch = (list.indices).firstNotNullOf { i ->
				val b = if (i == 0) lc else "$lc ($i)"
				if (branchedChapters[b]?.get(volume to number) == null) b else null
			}
			val chapter = MangaChapter(
				id = generateUid(id),
				title = attrs.getStringOrNull("title"),
				number = number,
				volume = volume,
				url = id,
				scanlator = team,
				uploadDate = dateFormat.parseSafe(attrs.getString("publishAt")),
				branch = branch,
				source = source,
			)
			if (chaptersBuilder.add(chapter)) {
				branchedChapters.getOrPut(branch, ::HashMap)[volume to number] = chapter
			}
		}
		return chaptersBuilder.toList()
	}

	private fun JSONArray.associateByKey(key: String): Map<String, List<JSONObject>> {
		val destination = LinkedHashMap<String, MutableList<JSONObject>>(length())
		repeat(length()) { i ->
			val item = getJSONObject(i)
			val keyValue = item.getString(key)
			destination.computeIfAbsent(keyValue) { mutableListOf() }.add(item)
		}
		return destination
	}

	private class Chapters(
		val data: List<JSONObject>,
		val total: Int,
	) {

		val size: Int
			get() = data.size
	}
}
