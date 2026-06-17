package com.kosen.reader.parsers.site.pt

import androidx.collection.ArraySet
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.core.PagedMangaParser
import com.kosen.reader.parsers.exception.ParseException
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.network.UserAgents
import com.kosen.reader.parsers.util.*
import com.kosen.reader.parsers.util.json.getDoubleOrDefault
import com.kosen.reader.parsers.util.json.getStringOrNull
import com.kosen.reader.parsers.util.json.mapJSON
import com.kosen.reader.parsers.util.json.mapJSONNotNull
import com.kosen.reader.parsers.util.suspendlazy.suspendLazy
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("SSSSCANLATOR", "Yomu", "pt")
internal class SssScanlator(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.SSSSCANLATOR, pageSize = 20) {

	override val configKeyDomain = ConfigKey.Domain("yomu.com.br")

	override val sourceLocale: Locale = Locale("pt", "BR")

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
	}

	override val userAgentKey: ConfigKey.UserAgent = ConfigKey.UserAgent(UserAgents.CHROME_MOBILE)

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("User-Agent", config[userAgentKey])
		.add("Origin", "https://$domain")
		.add("Referer", "https://$domain/")
		.add("Accept-Language", ACCEPT_LANGUAGE)
		.build()

	private fun getApiHeaders(): Headers = getRequestHeaders().newBuilder()
		.set("Accept", "application/json")
		.add("Sec-Fetch-Dest", "empty")
		.add("Sec-Fetch-Mode", "cors")
		.add("Sec-Fetch-Site", "same-origin")
		.build()

	private fun getChapterApiHeaders(chapterId: String): Headers = getApiHeaders().newBuilder()
		.add("x-ym-req", buildYmReqToken(chapterId))
		.build()

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

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = fetchTags(),
		availableStates = EnumSet.of(MangaState.ONGOING, MangaState.FINISHED, MangaState.PAUSED),
		availableContentTypes = EnumSet.of(
			ContentType.MANGA,
			ContentType.MANHWA,
			ContentType.MANHUA,
		),
	)

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = buildString {
			append("https://")
			append(domain)
			append("/api/library?page=")
			append(page.toString())
			append("&limit=")
			append(pageSize.toString())
			append("&sort=")
			append(
				when (order) {
					SortOrder.UPDATED -> "recent"
					SortOrder.POPULARITY -> "popular"
					SortOrder.ALPHABETICAL -> "title"
					else -> "recent"
				},
			)
			if (!filter.query.isNullOrEmpty()) {
				append("&search=")
				append(filter.query.urlEncoded())
			}
			filter.tags.firstOrNull()?.let { tag ->
				append("&genre=")
				append(tag.key.urlEncoded())
			}
			filter.states.oneOrThrowIfMany()?.let { state ->
				append("&status=")
				append(
					when (state) {
						MangaState.ONGOING -> "Ongoing"
						MangaState.FINISHED -> "Completed"
						MangaState.PAUSED -> "Hiatus"
						else -> ""
					},
				)
			}
			filter.types.oneOrThrowIfMany()?.let { type ->
				append("&type=")
				append(
					when (type) {
						ContentType.MANGA -> "manga"
						ContentType.MANHWA -> "manhwa"
						ContentType.MANHUA -> "manhua"
						else -> ""
					},
				)
			}
		}

		val json = webClient.httpGet(url, getApiHeaders()).parseJson()
		val data = json.optLibraryArray() ?: return emptyList()
		return data.mapJSONNotNull { obj ->
			val slug = obj.getString("slug")
			if (slug == "bloqueado" || isTrapAsset(obj.optString("cover"))) {
				return@mapJSONNotNull null
			}
			val relUrl = "/obra/$slug"
			Manga(
				id = generateUid(relUrl),
				title = obj.getString("title"),
				altTitles = emptySet(),
				url = relUrl,
				publicUrl = "https://$domain$relUrl",
				rating = obj.getDoubleOrDefault("rating", -10.0).let {
					if (it < 0) RATING_UNKNOWN else (it / 10.0).toFloat()
				},
				contentRating = null,
				coverUrl = obj.getStringOrNull("cover")?.takeUnless { isTrapAsset(it) },
				tags = emptySet(),
				state = null,
				authors = emptySet(),
				largeCoverUrl = null,
				description = null,
				source = source,
			)
		}
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val slug = manga.url.removePrefix("/obra/").trimEnd('/')
		val html = webClient.httpGet(manga.url.toAbsoluteUrl(domain), getRequestHeaders()).parseHtml().html()
		val libraryObra = fetchObraFromLibrary(slug)
		val pageMeta = parseObraPageMetadata(html, slug)
		val rsc = extractRscPayload(html)

		val totalChapters = libraryObra?.optInt("chapters", 0)?.takeIf { it > 0 }
			?: pageMeta?.chapterTotal
			?: 0
		val recentChapters = libraryObra?.optJSONArray("recentChapters")
		val chapterIdMap = getChapterIdMap(slug, html)
		val chapters = when {
			totalChapters > 0 -> buildChaptersFromLibrary(slug, totalChapters, recentChapters, chapterIdMap)
			isTrapPayload(rsc) -> emptyList()
			else -> parseChaptersFromRsc(rsc)
				.filterNot { isTrapChapter(it) }
				.ifEmpty { parseChaptersFromHtml(org.jsoup.Jsoup.parse(html, manga.url.toAbsoluteUrl(domain)), slug) }
		}.sortedBy { it.number }

		if (chapters.isEmpty() && pageMeta == null && libraryObra == null) {
			throw ParseException("Não foi possível carregar os detalhes da obra", manga.url)
		}

		val title = libraryObra?.getStringOrNull("title")?.takeIf { it.isNotBlank() }
			?: pageMeta?.title
			?: manga.title
		val coverImage = libraryObra?.getStringOrNull("cover")?.takeUnless { isTrapAsset(it) }
			?: pageMeta?.coverImage
			?: manga.coverUrl?.takeUnless { isTrapAsset(it) }
		val description = if (isTrapPayload(rsc)) {
			manga.description?.takeUnless { isTrapDescription(it) }
		} else {
			org.jsoup.Jsoup.parse(html).selectFirst("meta[property=og:description]")?.attr("content")
				?.takeUnless { it.isBlank() || isTrapDescription(it) }
				?: extractJsonString(rsc, "description")?.takeUnless { isTrapDescription(it) }
				?: manga.description?.takeUnless { isTrapDescription(it) }
		}
		val author = if (isTrapPayload(rsc)) null else extractJsonString(rsc, "author")
		val artist = if (isTrapPayload(rsc)) null else extractJsonString(rsc, "artist")
		val authors = buildSet {
			author?.takeUnless { it.isBlank() }?.let(::add)
			artist?.takeUnless { it.isBlank() || it == author }?.let(::add)
		}

		return manga.copy(
			title = title,
			description = description,
			authors = authors,
			coverUrl = coverImage,
			largeCoverUrl = coverImage,
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		if (chapter.url.startsWith("/api/chapters")) {
			tryFetchPagesFromApi(chapter.url).takeIf { it.isNotEmpty() }?.let { return it }
		}
		val match = CHAPTER_URL_REGEX.matchEntire(chapter.url)
		if (match != null) {
			val slug = resolveObraSlug(match.groupValues[1])
			val chapterNumber = match.groupValues[2]
			resolveChapterId(slug, chapterNumber)?.let { chapterId ->
				tryFetchPagesFromApi("/api/chapters?id=$chapterId").takeIf { it.isNotEmpty() }?.let { return it }
			}
			fetchPagesFromReader(slug, chapterNumber).takeIf { it.isNotEmpty() }?.let { return it }
		} else if (chapter.url.startsWith("/api/chapters")) {
			val chapterId = chapter.url.substringAfter("id=", "").substringBefore('&')
			if (chapterId.isNotBlank()) {
				fetchPagesFromReaderByChapterId(chapterId, chapter.number)?.takeIf { it.isNotEmpty() }?.let { return it }
			}
		}
		throw ParseException("Não foi possível carregar as páginas do capítulo", chapter.url)
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		if (page.url.startsWith("http://") || page.url.startsWith("https://")) {
			return page.url
		}
		return super.getPageUrl(page)
	}

	private suspend fun tryFetchPagesFromApi(chapterUrl: String): List<MangaPage> =
		runCatching { fetchPagesFromApi(chapterUrl) }.getOrDefault(emptyList())

	private suspend fun fetchRawHtml(url: String): String =
		webClient.httpGet(url, getRequestHeaders()).parseRaw()

	private suspend fun fetchPagesFromApi(chapterUrl: String): List<MangaPage> {
		val chapterId = chapterUrl.substringAfter("id=", "").substringBefore('&').takeIf { it.isNotBlank() }
			?: throw ParseException("ID de capítulo inválido", chapterUrl)
		val json = webClient.httpGet(
			chapterUrl.toAbsoluteUrl(domain),
			getChapterApiHeaders(chapterId),
		).parseJson()
		json.optString("error").takeUnless { it.isBlank() }?.let { message ->
			throw ParseException(message, chapterUrl)
		}
		if (json.optBoolean("isLocked")) {
			throw ParseException("Este capítulo ainda não foi lançado ou está bloqueado.", chapterUrl)
		}
		val chapter = json.optJSONObject("chapter")
			?: throw ParseException("Resposta inválida da API de capítulos", chapterUrl)
		val content = chapter.optJSONArray("content")
			?: throw ParseException("Capítulo sem páginas", chapterUrl)
		val result = ArrayList<MangaPage>(content.length())
		for (i in 0 until content.length()) {
			val url = content.getString(i)
			if (isTrapAsset(url)) {
				continue
			}
			result.add(
				MangaPage(
					id = generateUid(url),
					url = url,
					preview = null,
					source = source,
				),
			)
		}
		if (result.isEmpty()) {
			throw ParseException("Capítulo sem páginas", chapterUrl)
		}
		return result
	}

	private suspend fun fetchPagesFromReader(slug: String, chapterNumber: String): List<MangaPage> {
		resolveChapterIdFromReader(slug, chapterNumber)?.let { chapterId ->
			tryFetchPagesFromApi("/api/chapters?id=$chapterId").takeIf { it.isNotEmpty() }?.let { return it }
		}

		val rawHtml = fetchRawHtml("https://$domain/ler/$slug/$chapterNumber")
		val urls = LinkedHashSet<String>()
		PRELOAD_IMAGE_REGEX.findAll(rawHtml).forEach { match ->
			if (!isTrapAsset(match.groupValues[1])) {
				urls.add(match.groupValues[1])
			}
		}
		CDN_PAGE_REGEX.findAll(rawHtml).forEach { match ->
			if (!isTrapAsset(match.value)) {
				urls.add(match.value)
			}
		}
		val rsc = extractRscPayload(rawHtml)
		CDN_PAGE_REGEX.findAll(rsc).forEach { match ->
			if (!isTrapAsset(match.value)) {
				urls.add(match.value)
			}
		}
		IMAGENS_LISTA_REGEX.find(rsc)?.groupValues?.get(1)?.let { listBody ->
			Regex("""https://[^"\\]+""").findAll(listBody).forEach { match ->
				if (!isTrapAsset(match.value)) {
					urls.add(match.value)
				}
			}
		}
		return urls.map { url ->
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}

	private suspend fun fetchPagesFromReaderByChapterId(chapterId: String, chapterNumber: Float): List<MangaPage>? {
		tryFetchPagesFromApi("/api/chapters?id=$chapterId").takeIf { it.isNotEmpty() }?.let { return it }
		return null
	}

	private suspend fun resolveObraSlug(slug: String): String {
		if (getChapterIdMap(slug).isNotEmpty()) {
			return slug
		}
		val query = slug.replace('-', ' ').urlEncoded()
		val json = webClient.httpGet(
			"https://$domain/api/library?search=$query&limit=20",
			getApiHeaders(),
		).parseJson()
		val arr = json.optLibraryArray() ?: return slug
		for (i in 0 until arr.length()) {
			val obra = arr.optJSONObject(i) ?: continue
			val candidate = obra.optString("slug")
			if (candidate.isBlank()) continue
			if (candidate == slug || slugSimilar(slug, candidate)) {
				chapterIdCache.remove(slug)
				return candidate
			}
		}
		return slug
	}

	private fun slugSimilar(requested: String, candidate: String): Boolean {
		if (requested == candidate) return true
		if (requested.length < 8 || candidate.length < 8) return false
		val minLen = minOf(requested.length, candidate.length)
		var same = 0
		for (i in 0 until minLen) {
			if (requested[i] == candidate[i]) same++
		}
		return same >= minLen - 2
	}

	private suspend fun resolveChapterId(slug: String, chapterNumber: String): String? {
		getChapterIdMap(slug)[chapterNumber]?.let { return it }
		val json = webClient.httpGet(
			"https://$domain/api/library?search=${slug.replace('-', ' ').urlEncoded()}&limit=20",
			getApiHeaders(),
		).parseJson()
		val arr = json.optLibraryArray() ?: return resolveChapterIdFromReader(slug, chapterNumber)
		for (i in 0 until arr.length()) {
			val obra = arr.optJSONObject(i) ?: continue
			if (obra.optString("slug") != slug) continue
			val recent = obra.optJSONArray("recentChapters") ?: break
			for (j in 0 until recent.length()) {
				val ch = recent.optJSONObject(j) ?: continue
				if (ch.optString("number") == chapterNumber) {
					return ch.getStringOrNull("id")
				}
			}
			break
		}
		return resolveChapterIdFromReader(slug, chapterNumber)
	}

	private suspend fun getChapterIdMap(slug: String, obraHtml: String? = null): Map<String, String> {
		chapterIdCache[slug]?.let { return it }
		val map = LinkedHashMap<String, String>()
		val libraryObra = fetchObraFromLibrary(slug)
		libraryObra?.optJSONArray("recentChapters")?.let { recent ->
			map.putAll(parseChapterIdMapFromJsonArray(recent))
		}
		val html = obraHtml ?: fetchRawHtml("https://$domain/obra/$slug")
		map.putAll(parseChapterIdMapFromHtml(html))
		if (map.isEmpty()) {
			val rsc = extractRscPayload(html)
			if (!isTrapPayload(rsc)) {
				map.putAll(parseChapterIdMapFromRsc(rsc))
				parseChaptersFromRsc(rsc).forEach { ch ->
					if (!isTrapChapter(ch)) {
						val numKey = chapterNumberKey(ch.number)
						val id = ch.url.removePrefix("/api/chapters?id=")
						if (id.isNotBlank() && !isTrapChapterId(id)) {
							map.putIfAbsent(numKey, id)
						}
					}
				}
			}
		}
		chapterIdCache[slug] = map
		return map
	}

	private fun parseChapterIdMapFromJsonArray(array: JSONArray): Map<String, String> {
		val map = LinkedHashMap<String, String>()
		for (i in 0 until array.length()) {
			val ch = array.optJSONObject(i) ?: continue
			val number = ch.optString("number")
			val id = ch.getStringOrNull("id") ?: continue
			if (number.isNotBlank() && !isTrapChapterId(id)) {
				map[number] = id
				number.toFloatOrNull()?.let { f -> map.putIfAbsent(chapterNumberKey(f), id) }
			}
		}
		return map
	}

	private fun parseChapterIdMapFromHtml(html: String): Map<String, String> {
		val map = LinkedHashMap<String, String>()
		for (match in CHAPTER_LIST_ENTRY_REGEX.findAll(html)) {
			val number = match.groupValues[1]
			val id = match.groupValues[2]
			if (number.isNotBlank() && !isTrapChapterId(id)) {
				map[number] = id
				number.toFloatOrNull()?.let { f -> map.putIfAbsent(chapterNumberKey(f), id) }
			}
		}
		return map
	}

	private suspend fun fetchObraFromLibrary(slug: String): JSONObject? {
		buildLibrarySearchQueries(slug).forEach { query ->
			findObraInLibraryResponse(
				webClient.httpGet(
					"https://$domain/api/library?search=${query.urlEncoded()}&limit=20",
					getApiHeaders(),
				).parseJson(),
				slug,
			)?.let { return it }
		}
		for (page in 1..5) {
			findObraInLibraryResponse(
				webClient.httpGet(
					"https://$domain/api/library?page=$page&limit=50&sort=recent",
					getApiHeaders(),
				).parseJson(),
				slug,
			)?.let { return it }
		}
		return null
	}

	private fun buildLibrarySearchQueries(slug: String): List<String> {
		val words = slug.split('-').filter { it.isNotBlank() }
		val queries = LinkedHashSet<String>()
		queries.add(slug.replace('-', ' '))
		queries.add(slug)
		for (size in 2..minOf(4, words.size)) {
			queries.add(words.takeLast(size).joinToString(" "))
		}
		return queries.toList()
	}

	private fun findObraInLibraryResponse(json: JSONObject, slug: String): JSONObject? {
		val arr = json.optLibraryArray() ?: return null
		for (i in 0 until arr.length()) {
			val obra = arr.optJSONObject(i) ?: continue
			if (obra.optString("slug") == slug) {
				return obra
			}
		}
		return null
	}

	private fun parseObraPageMetadata(html: String, slug: String): ObraPageMetadata? {
		val refMatch = Regex(
			"""refId\\":\\"([^\\]+)\\",\\"slug\\":\\"${Regex.escape(slug)}\\"[^}]*chapterTotal\\":(\d+)""",
		).find(html) ?: return null
		val refId = refMatch.groupValues[1]
		val chapterTotal = refMatch.groupValues[2].toIntOrNull() ?: return null
		val title = Regex(
			"""seriesId\\":\\"${Regex.escape(refId)}\\",\\"title\\":\\"((?:[^\\]|\\.)*)\\"""",
		).find(html)?.groupValues?.get(1)
			?.let(::decodeEscapes)
			?.takeIf { it.isNotBlank() }
		val coverImage = Regex(
			"""coverImage\\":\\"(https://cdn\.(?:monstercomics|yomu)\.com\.br/obras/${Regex.escape(slug)}[^\\]+)\\"""",
		).find(html)?.groupValues?.get(1)
			?.takeUnless { isTrapAsset(it) }
		return ObraPageMetadata(
			refId = refId,
			chapterTotal = chapterTotal,
			title = title,
			coverImage = coverImage,
		)
	}

	private data class ObraPageMetadata(
		val refId: String,
		val chapterTotal: Int,
		val title: String?,
		val coverImage: String?,
	)

	private fun buildChaptersFromLibrary(
		slug: String,
		totalChapters: Int,
		recentChapters: JSONArray?,
		chapterIdMap: Map<String, String> = emptyMap(),
	): List<MangaChapter> {
		val idByNumber = LinkedHashMap<String, String>()
		idByNumber.putAll(chapterIdMap)
		if (recentChapters != null) {
			idByNumber.putAll(parseChapterIdMapFromJsonArray(recentChapters))
		}
		val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", sourceLocale)
		return (1..totalChapters).map { index ->
			val numberKey = index.toString()
			val known = idByNumber[numberKey]
			val chapterUrl = known?.let { "/api/chapters?id=$it" } ?: "/ler/$slug/$numberKey"
			val recent = recentChapters?.let { array ->
				(0 until array.length())
					.mapNotNull { array.optJSONObject(it) }
					.firstOrNull { it.optString("number") == numberKey }
			}
			MangaChapter(
				id = generateUid(chapterUrl),
				title = recent?.getStringOrNull("chapterSlug")?.replace('-', ' ')?.replaceFirstChar {
					if (it.isLowerCase()) it.titlecase(sourceLocale) else it.toString()
				},
				number = index.toFloat(),
				volume = 0,
				url = chapterUrl,
				scanlator = null,
				uploadDate = recent?.getStringOrNull("releaseAt")
					?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() } ?: 0L,
				branch = null,
				source = source,
			)
		}
	}

	private suspend fun fetchChaptersFallback(slug: String): List<MangaChapter> {
		val response = webClient.httpGet("https://$domain/obra/$slug", getRequestHeaders())
		if (!response.isSuccessful) {
			response.close()
			return emptyList()
		}
		val doc = response.parseHtml()
		val rsc = extractRscPayload(doc.html())
		response.close()
		if (isTrapPayload(rsc)) {
			return emptyList()
		}
		return parseChaptersFromRsc(rsc)
			.filterNot { isTrapChapter(it) }
			.ifEmpty { parseChaptersFromHtml(doc, slug) }
	}

	private suspend fun resolveChapterIdFromReader(slug: String, chapterNumber: String): String? {
		val html = fetchRawHtml("https://$domain/ler/$slug/$chapterNumber")
		for (pattern in READER_CHAPTER_ID_PATTERNS) {
			val regex = Regex(pattern.replace("%NUM%", Regex.escape(chapterNumber)))
			regex.find(html)?.groupValues?.get(1)?.let { id ->
				if (!isTrapChapterId(id)) {
					return id
				}
			}
		}
		return null
	}

	private fun buildYmReqToken(chapterId: String): String {
		val date = ymReqDateFormat.format(Date())
		val raw = "yk-v3-$chapterId-$date"
		return context.encodeBase64(raw.toByteArray(Charsets.UTF_8))
			.replace('+', '-')
			.replace('/', '_')
			.replace("=", "")
			.take(24)
	}

	private fun isTrapPayload(text: String): Boolean =
		text.contains("aviso-scraper", ignoreCase = true) ||
			text.contains("bloqueado contra scrapers", ignoreCase = true) ||
			text.contains("fake-cap", ignoreCase = true)

	private fun isTrapDescription(text: String): Boolean =
		text.contains("bloqueado contra scrapers", ignoreCase = true)

	private fun isTrapAsset(url: String): Boolean =
		url.contains("aviso-scraper", ignoreCase = true) ||
			url.contains("vampeta", ignoreCase = true)

	private fun isTrapChapterId(id: String): Boolean =
		id.startsWith("fake", ignoreCase = true)

	private fun isTrapChapter(chapter: MangaChapter): Boolean =
		isTrapChapterId(chapter.url.removePrefix("/api/chapters?id=")) ||
			chapter.number >= 9999f

	private fun parseChapterIdMapFromRsc(rsc: String): Map<String, String> {
		val map = LinkedHashMap<String, String>()
		for (match in CHAPTER_ENTRY_REGEX.findAll(rsc)) {
			val number = match.groupValues[1]
			val id = match.groupValues[2]
			map[number] = id
			number.toFloatOrNull()?.let { f -> map.putIfAbsent(chapterNumberKey(f), id) }
		}
		return map
	}

	private fun chapterNumberKey(number: Float): String =
		if (number == number.toLong().toFloat()) number.toLong().toString() else number.toString()

	private fun parseChaptersFromHtml(doc: Document, slug: String): List<MangaChapter> {
		val selector = "a[href^=/ler/$slug/]"
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", sourceLocale)
		return doc.select(selector).mapNotNull { anchor ->
			val href = anchor.attr("href").trim()
			val numberStr = href.substringAfterLast('/')
			val number = numberStr.toFloatOrNull() ?: return@mapNotNull null
			val title = anchor.selectFirst("[title]")?.attr("title")?.takeUnless { it.isBlank() }
				?: anchor.text().takeIf { it.isNotBlank() }
			val uploadDate = anchor.selectFirst("time[datetime]")?.attr("datetime")
				?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() }
				?: 0L
			MangaChapter(
				id = generateUid(href),
				title = title,
				number = number,
				volume = 0,
				url = href,
				scanlator = null,
				uploadDate = uploadDate,
				branch = null,
				source = source,
			)
		}.distinctBy { it.url }
	}

	private fun parseChaptersFromRsc(rsc: String): List<MangaChapter> {
		val chaptersJson = extractJsonArray(rsc, "chapters")
			?: extractJsonArray(rsc, "capitulos_lista")
		if (chaptersJson != null) {
			return mapChaptersFromJsonArray(chaptersJson)
		}
		return parseChapterIdMapFromRsc(rsc).map { (number, id) ->
			val chapterUrl = "/api/chapters?id=$id"
			MangaChapter(
				id = generateUid(chapterUrl),
				title = null,
				number = number.toFloatOrNull() ?: 0f,
				volume = 0,
				url = chapterUrl,
				scanlator = null,
				uploadDate = 0L,
				branch = null,
				source = source,
			)
		}
	}

	private fun mapChaptersFromJsonArray(chaptersJson: org.json.JSONArray): List<MangaChapter> {
		val dateFormat = SimpleDateFormat("dd/MM/yyyy", sourceLocale)
		return chaptersJson.mapJSONNotNull { obj ->
			val id = obj.getStringOrNull("id") ?: return@mapJSONNotNull null
			val number = obj.getDoubleOrDefault("number", 0.0).toFloat()
			val chapterUrl = "/api/chapters?id=$id"
			MangaChapter(
				id = generateUid(chapterUrl),
				title = obj.getStringOrNull("title"),
				number = number,
				volume = 0,
				url = chapterUrl,
				scanlator = obj.getStringOrNull("scanName")?.takeUnless { it == "Desconhecido" },
				uploadDate = obj.getStringOrNull("releaseDate")
					?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() } ?: 0L,
				branch = null,
				source = source,
			)
		}
	}

	private val chapterIdCache = java.util.concurrent.ConcurrentHashMap<String, Map<String, String>>()

	private val tagsCache = suspendLazy(initializer = ::loadTags)

	private suspend fun fetchTags(): Set<MangaTag> = tagsCache.get()

	private suspend fun loadTags(): Set<MangaTag> {
		val arr = webClient.httpGet("https://$domain/api/genres", getApiHeaders()).parseJsonArray()
		val result = ArraySet<MangaTag>(arr.length())
		for (i in 0 until arr.length()) {
			val name = arr.getString(i)
			result.add(
				MangaTag(
					title = name.toTitleCase(sourceLocale),
					key = name,
					source = source,
				),
			)
		}
		return result
	}

	private fun extractRscPayload(html: String): String {
		val regex = Regex("""self\.__next_f\.push\(\[1,"((?:[^"\\]|\\.)*)"\]\)""")
		val builder = StringBuilder()
		for (match in regex.findAll(html)) {
			builder.append(decodeEscapes(match.groupValues[1]))
		}
		return builder.toString()
	}

	private fun decodeEscapes(input: String): String {
		val sb = StringBuilder(input.length)
		var i = 0
		while (i < input.length) {
			val char = input[i]
			if (char == '\\' && i + 1 < input.length) {
				when (val next = input[i + 1]) {
					'n' -> sb.append('\n')
					't' -> sb.append('\t')
					'r' -> sb.append('\r')
					'"' -> sb.append('"')
					'\\' -> sb.append('\\')
					'/' -> sb.append('/')
					'b' -> sb.append('\b')
					'f' -> sb.append('\u000C')
					'u' -> if (i + 5 < input.length) {
						val hex = input.substring(i + 2, i + 6)
						runCatching { sb.append(hex.toInt(16).toChar()) }.getOrElse { sb.append(next) }
						i += 4
					} else {
						sb.append(next)
					}
					else -> sb.append(next)
				}
				i += 2
			} else {
				sb.append(char)
				i++
			}
		}
		return sb.toString()
	}

	private fun extractJsonString(text: String, key: String): String? {
		val pattern = Regex("\"" + Regex.escape(key) + "\":\"((?:[^\"\\\\]|\\\\.)*)\"")
		val match = pattern.find(text) ?: return null
		return decodeEscapes(match.groupValues[1]).takeUnless { it.isBlank() }
	}

	private fun extractJsonArray(text: String, key: String): org.json.JSONArray? {
		val keyPattern = "\"$key\":["
		val startIndex = text.indexOf(keyPattern)
		if (startIndex < 0) return null
		var i = startIndex + keyPattern.length - 1
		var depth = 0
		var inString = false
		var escaped = false
		val arrayStart = i
		while (i < text.length) {
			val char = text[i]
			if (inString) {
				if (escaped) escaped = false
				else if (char == '\\') escaped = true
				else if (char == '"') inString = false
			} else {
				when (char) {
					'"' -> inString = true
					'[' -> depth++
					']' -> {
						depth--
						if (depth == 0) {
							val slice = text.substring(arrayStart, i + 1)
							return runCatching { org.json.JSONArray(slice) }.getOrNull()
						}
					}
				}
			}
			i++
		}
		return null
	}

	private fun org.json.JSONObject.optLibraryArray(): org.json.JSONArray? =
		optJSONArray("prateleira")
			?: optJSONArray("acervo")
			?: optJSONArray("obras")
			?: optJSONArray("data")
			?: optEncodedLibraryArray("garimpo")
			?: optEncodedLibraryArray("catalogo")
			?: optJSONArray("catalogo")

	private fun org.json.JSONObject.optEncodedLibraryArray(key: String): org.json.JSONArray? {
		val encoded = optString(key).takeUnless { it.isBlank() } ?: return null
		return runCatching {
			org.json.JSONArray(context.decodeBase64(encoded).toString(Charsets.UTF_8))
		}.getOrNull()
	}

	private companion object {
		private const val ACCEPT_LANGUAGE = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7"
		private val ymReqDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
			timeZone = TimeZone.getTimeZone("UTC")
		}
		val READER_CHAPTER_ID_PATTERNS = listOf(
			"""\\"chapter\\":\{\\"id\\":\\"([^\\]+)\\"[^}]*\\"number\\":%NUM%\b""",
			"""\"chapter\":\{\"id\":\"([^\"]+)\"[^}]*\"number\":%NUM%\b""",
			"""\\"id\\":\\"([^\\]+)\\"[^}]*\\"number\\":%NUM%\b""",
			"""\"id\":\"([^\"]+)\"[^}]*\"number\":%NUM%\b""",
		)
		val CHAPTER_LIST_ENTRY_REGEX = Regex(
			"""\\?"number\\?":(\d+(?:\.\d+)?)[^}]{0,500}?\\?"id\\?":\\?"([^\\]+)\\?"""",
		)
		val PRELOAD_IMAGE_REGEX = Regex(
			"""<link[^>]+rel="preload"[^>]+as="image"[^>]+href="(https://cdn\.(?:monstercomics|yomu)\.com\.br/[^"]+\.(?:webp|jpg|jpeg|png))"""",
			RegexOption.IGNORE_CASE,
		)
		val CHAPTER_URL_REGEX = Regex("""^/ler/([^/]+)/([^/]+)$""")
		val CHAPTER_ENTRY_REGEX = Regex("""\{"number":(\d+(?:\.\d+)?).*?"id":"([^"]+)"""")
		val IMAGENS_LISTA_REGEX = Regex(""""imagens_lista":\[([^\]]*)\]""")
		val CDN_PAGE_REGEX = Regex(
			"""https://cdn\.(?:monstercomics|yomu)\.com\.br/[^"'\s<>\\]+\.(?:webp|jpg|jpeg|png)""",
		)
	}
}
