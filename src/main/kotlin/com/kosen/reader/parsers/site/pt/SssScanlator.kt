package com.kosen.reader.parsers.site.pt

import androidx.collection.ArraySet
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
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
	PagedMangaParser(context, MangaParserSource.SSSSCANLATOR, pageSize = 30) {

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
		fetchTaurusList(page, order, filter)?.let { return it }
		val json = fetchLibraryJson(page, order, filter)
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
		val slug = manga.url.removePrefix("/obra/").substringBefore('?').trimEnd('/')
		fetchTaurusDetails(slug, manga)?.takeIf { !it.chapters.isNullOrEmpty() }?.let { return it }
		val pageUrl = manga.url.toAbsoluteUrl(domain)
		val html = runCatching { fetchRawHtml(pageUrl) }.getOrNull()
		val doc = html?.let { org.jsoup.Jsoup.parse(it, pageUrl) }
		val pageMeta = html?.let { parseObraPageMetadata(it, slug) }
		val rsc = html?.let { extractRscPayload(it) }.orEmpty()
		val htmlChapters = buildList {
			if (doc != null) {
				addAll(parseChaptersFromHtml(doc, slug))
			}
			if (isEmpty() && !html.isNullOrEmpty()) {
				addAll(parseChaptersFromHtmlText(html, slug))
			}
		}
		val libraryObra = if (htmlChapters.isEmpty()) {
			fetchObraFromLibrary(slug, manga.title)
		} else {
			null
		}
		val idMap = LinkedHashMap<String, String>()
		libraryObra?.optJSONArray("recentChapters")?.let { recent ->
			idMap.putAll(parseChapterIdMapFromJsonArray(recent))
		}
		if (!html.isNullOrEmpty()) {
			idMap.putAll(parseChapterIdMapFromHtml(html))
		}
		if (!isTrapPayload(rsc)) {
			idMap.putAll(parseChapterIdMapFromRsc(rsc))
		}

		val rscChapters = if (isTrapPayload(rsc)) {
			emptyList()
		} else {
			parseChaptersFromRsc(rsc).filterNot { isTrapChapter(it) }
		}
		val totalChapters = libraryObra?.optInt("chapters", 0)?.takeIf { it > 0 }
			?: pageMeta?.chapterTotal
			?: 0
		val recentChapters = libraryObra?.optJSONArray("recentChapters")
		val chapters = when {
			htmlChapters.isNotEmpty() -> applyChapterIds(htmlChapters, idMap)
			rscChapters.isNotEmpty() -> rscChapters
			totalChapters > 0 -> buildChaptersFromLibrary(slug, totalChapters, recentChapters, idMap)
			recentChapters != null && recentChapters.length() > 0 -> mapRecentLibraryChapters(slug, recentChapters)
			else -> emptyList()
		}.sortedBy { it.number }

		if (chapters.isEmpty()) {
			fetchDetailsViaJs(slug, manga)?.takeIf { !it.chapters.isNullOrEmpty() }?.let { return it }
		}
		if (chapters.isEmpty() && pageMeta == null && libraryObra == null && html.isNullOrEmpty()) {
			fetchTaurusDetails(slug, manga)?.let { return it }
			throw ParseException("Não foi possível carregar os capítulos desta obra. Tente de novo em alguns segundos.", manga.url)
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
			doc?.selectFirst("meta[property=og:description]")?.attr("content")
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
		val match = CHAPTER_URL_REGEX.matchEntire(chapter.url.substringBefore('?'))
		if (match != null) {
			tryFetchTaurusPages(match.groupValues[1], match.groupValues[2])
				.takeIf { it.isNotEmpty() }
				?.let { return it }
		}
		chapterApiId(chapter.url)?.let { chapterId ->
			tryFetchPagesFromApi("/api/chapters?id=$chapterId").takeIf { it.isNotEmpty() }?.let { return it }
		}
		if (match != null) {
			fetchPagesFromReader(match.groupValues[1], match.groupValues[2])
				.takeIf { it.isNotEmpty() }
				?.let { return it }
			fetchPagesViaJs(match.groupValues[1], match.groupValues[2])
				.takeIf { it.isNotEmpty() }
				?.let { return it }
		}
		throw ParseException("Não foi possível carregar as páginas do capítulo", chapter.url)
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		val url = if (page.url.startsWith("http://") || page.url.startsWith("https://")) {
			page.url
		} else {
			super.getPageUrl(page)
		}
		if (url.contains("/api/chapter/secure-image")) {
			return "https://$domain/api/proxy-image?q=${url.urlEncoded()}"
		}
		return url
	}

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val url = request.url
		val path = url.encodedPath
		val builder = request.newBuilder()
		var changed = false
		if (path.contains("/api/chapters") && request.header("x-ym-req") == null) {
			url.queryParameter("id")?.takeIf { it.isNotBlank() }?.let { chapterId ->
				builder.header("x-ym-req", buildYmReqToken(chapterId))
				changed = true
			}
		}
		if (path.contains("/api/proxy-image") && request.header("x-ym-media") == null) {
			val mediaUrl = url.queryParameter("q").orEmpty()
			if (mediaUrl.isNotEmpty()) {
				builder.header("x-ym-media", buildYmMediaToken(mediaUrl))
				changed = true
			}
		}
		return chain.proceed(if (changed) builder.build() else request)
	}

	private suspend fun tryFetchPagesFromApi(chapterUrl: String): List<MangaPage> =
		runCatching { fetchPagesFromApi(chapterUrl) }.getOrDefault(emptyList())

	private fun getTaurusHeaders(): Headers = Headers.Builder()
		.add("User-Agent", config[userAgentKey])
		.add("Accept", "application/json")
		.add("Origin", "https://$domain")
		.add("Referer", "https://$domain/")
		.build()

	private suspend fun fetchTaurusList(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga>? {
		val url = "$PUBLIC_API/list".toHttpUrl().newBuilder()
			.addQueryParameter("page", page.toString())
			.addQueryParameter(
				"sort",
				when (order) {
					SortOrder.POPULARITY -> "popular"
					SortOrder.ALPHABETICAL -> "az"
					else -> "updated"
				},
			)
			.apply {
				if (!filter.query.isNullOrEmpty()) {
					addQueryParameter("q", filter.query)
				}
				filter.tags.firstOrNull()?.let { addQueryParameter("genre", it.key) }
				filter.states.oneOrThrowIfMany()?.let { state ->
					val status = when (state) {
						MangaState.ONGOING -> "ONGOING"
						MangaState.FINISHED -> "COMPLETED"
						MangaState.PAUSED -> "HIATUS"
						else -> null
					}
					if (status != null) {
						addQueryParameter("status", status)
					}
				}
				filter.types.oneOrThrowIfMany()?.let { type ->
					val typeValue = when (type) {
						ContentType.MANGA -> "MANGA"
						ContentType.MANHWA -> "MANHWA"
						ContentType.MANHUA -> "MANHUA"
						else -> null
					}
					if (typeValue != null) {
						addQueryParameter("type", typeValue)
					}
				}
			}
			.build()
		val json = runCatching { webClient.httpGet(url, getTaurusHeaders()).parseJson() }.getOrNull()
			?: return null
		if (json.has("error")) return null
		val series = json.optJSONArray("series") ?: return null
		return series.mapJSONNotNull { obj ->
			val slug = obj.getStringOrNull("slug")?.takeIf { it.isNotBlank() } ?: return@mapJSONNotNull null
			if (slug == "bloqueado") return@mapJSONNotNull null
			val relUrl = "/obra/$slug"
			Manga(
				id = generateUid(relUrl),
				title = obj.getStringOrNull("title") ?: slug,
				altTitles = emptySet(),
				url = relUrl,
				publicUrl = "https://$domain$relUrl",
				rating = RATING_UNKNOWN,
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

	private suspend fun fetchTaurusDetails(slug: String, manga: Manga): Manga? = runCatching {
		val json = webClient.httpGet("$PUBLIC_API/manga/$slug", getTaurusHeaders()).parseJson()
		if (json.has("error") || json.optString("slug").isBlank() && json.optString("title").isBlank()) {
			return@runCatching null
		}
		parseTaurusManga(json, manga)
	}.getOrNull()

	private fun parseTaurusManga(json: JSONObject, manga: Manga): Manga {
		val slug = json.getStringOrNull("slug") ?: manga.url.removePrefix("/obra/").substringBefore('?')
		val genres = json.optJSONArray("genres")
		val tags = if (genres != null) parseGenreArray(genres) else manga.tags
		val authors = buildSet {
			json.getStringOrNull("author")?.takeIf { it.isNotBlank() }?.let(::add)
			json.getStringOrNull("artist")?.takeIf { it.isNotBlank() }?.let(::add)
		}
		val chapters = json.optJSONArray("chapters")?.mapJSONNotNull { ch ->
			val number = ch.optDouble("number", 0.0).toFloat()
			val label = if (number == number.toLong().toFloat()) number.toLong().toString() else number.toString()
			val id = ch.getStringOrNull("id")
			val chapterUrl = if (!id.isNullOrBlank()) {
				"/ler/$slug/$label?id=$id"
			} else {
				"/ler/$slug/$label"
			}
			MangaChapter(
				id = generateUid(chapterUrl),
				title = ch.getStringOrNull("title"),
				number = number,
				volume = 0,
				url = chapterUrl,
				scanlator = null,
				uploadDate = parseTaurusDate(ch.getStringOrNull("releaseAt") ?: ch.getStringOrNull("releaseDate")),
				branch = null,
				source = source,
			)
		}?.sortedBy { it.number }.orEmpty()
		val status = json.getStringOrNull("status")?.uppercase(Locale.ROOT)
		val cover = json.getStringOrNull("cover")?.takeUnless { isTrapAsset(it) } ?: manga.coverUrl
		return manga.copy(
			title = json.getStringOrNull("title")?.takeIf { it.isNotBlank() } ?: manga.title,
			url = "/obra/$slug",
			publicUrl = "https://$domain/obra/$slug",
			description = json.getStringOrNull("description") ?: manga.description,
			coverUrl = cover,
			largeCoverUrl = cover,
			authors = authors.ifEmpty { manga.authors },
			tags = tags.ifEmpty { manga.tags },
			state = when (status) {
				"ONGOING" -> MangaState.ONGOING
				"COMPLETED", "COMPLETE" -> MangaState.FINISHED
				"HIATUS" -> MangaState.PAUSED
				"CANCELED", "CANCELLED" -> MangaState.ABANDONED
				else -> manga.state
			},
			chapters = chapters,
		)
	}

	private fun parseTaurusDate(raw: String?): Long {
		val value = raw?.trim().orEmpty()
		if (value.isEmpty()) return 0L
		for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "dd/MM/yyyy", "yyyy-MM-dd")) {
			val parsed = runCatching {
				SimpleDateFormat(pattern, Locale.US).apply {
					if (pattern.contains("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
				}.parse(value)?.time
			}.getOrNull()
			if (parsed != null && parsed > 0L) return parsed
		}
		return 0L
	}

	private suspend fun tryFetchTaurusPages(slug: String, chapterNumber: String): List<MangaPage> = runCatching {
		val json = webClient.httpGet("$PUBLIC_API/chapter/$slug/$chapterNumber", getTaurusHeaders()).parseJson()
		if (json.has("error")) return@runCatching emptyList()
		val pages = json.optJSONArray("pages") ?: return@runCatching emptyList()
		(0 until pages.length()).mapNotNull { i ->
			val url = pages.optString(i).takeIf { it.isNotBlank() && !isTrapAsset(it) } ?: return@mapNotNull null
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}.getOrDefault(emptyList())

	private suspend fun fetchDetailsViaJs(slug: String, manga: Manga): Manga? {
		val pageUrl = "https://$domain/obra/$slug"
		val script = """
			(() => new Promise(resolve => {
				const collect = () => {
					const links = [...document.querySelectorAll('a[href*="/ler/"]')]
						.map(a => a.getAttribute('href') || '')
						.filter(href => href.includes('/ler/'));
					const title = document.querySelector('h1')?.textContent?.trim() || '';
					const description = document.querySelector('meta[property="og:description"]')?.getAttribute('content') || '';
					const cover = document.querySelector('meta[property="og:image"]')?.getAttribute('content') || '';
					if (links.length > 0) {
						return JSON.stringify({ title, description, cover, links: [...new Set(links)] });
					}
					return null;
				};
				const first = collect();
				if (first) return resolve(first);
				let attempts = 0;
				const timer = setInterval(() => {
					attempts += 1;
					const value = collect();
					if (value || attempts >= 40) {
						clearInterval(timer);
						resolve(value || JSON.stringify({ title: '', description: '', cover: '', links: [] }));
					}
				}, 250);
			}))()
		""".trimIndent()
		val raw = runCatching { context.evaluateJs(pageUrl, script, timeout = 25000L) }.getOrNull()
			?: return null
		val json = runCatching { JSONObject(raw.trim().removeSurrounding("\"").replace("\\\"", "\"")) }.getOrNull()
			?: runCatching { JSONObject(raw.trim()) }.getOrNull()
			?: return null
		val links = json.optJSONArray("links") ?: return null
		if (links.length() == 0) return null
		val chapters = (0 until links.length()).mapNotNull { i ->
			val href = links.optString(i).substringBefore('?').trim()
			val numberStr = href.substringAfterLast('/')
			val number = numberStr.toFloatOrNull() ?: return@mapNotNull null
			MangaChapter(
				id = generateUid(href),
				title = null,
				number = number,
				volume = 0,
				url = href,
				scanlator = null,
				uploadDate = 0L,
				branch = null,
				source = source,
			)
		}.distinctBy { it.url }.sortedBy { it.number }
		if (chapters.isEmpty()) return null
		val cover = json.getStringOrNull("cover")?.takeUnless { it.isBlank() || isTrapAsset(it) }
		return manga.copy(
			title = json.getStringOrNull("title")?.takeIf { it.isNotBlank() } ?: manga.title,
			description = json.getStringOrNull("description")?.takeUnless { it.isBlank() || isTrapDescription(it) }
				?: manga.description,
			coverUrl = cover ?: manga.coverUrl,
			largeCoverUrl = cover ?: manga.largeCoverUrl,
			chapters = chapters,
		)
	}

	private suspend fun fetchPagesViaJs(slug: String, chapterNumber: String): List<MangaPage> {
		val pageUrl = "https://$domain/ler/$slug/$chapterNumber"
		val script = """
			(() => new Promise(resolve => {
				const collect = () => {
					const urls = [
						...document.querySelectorAll('img[src*="cdn.yomu"], img[src*="cdn.monstercomics"], img[data-src*="cdn."]'),
					].map(img => img.getAttribute('src') || img.getAttribute('data-src') || '')
						.filter(src => /\.(webp|jpg|jpeg|png)(\?|${'$'})/i.test(src));
					if (urls.length > 1) return JSON.stringify([...new Set(urls)]);
					return null;
				};
				const first = collect();
				if (first) return resolve(first);
				let attempts = 0;
				const timer = setInterval(() => {
					attempts += 1;
					const value = collect();
					if (value || attempts >= 40) {
						clearInterval(timer);
						resolve(value || '[]');
					}
				}, 250);
			}))()
		""".trimIndent()
		val raw = runCatching { context.evaluateJs(pageUrl, script, timeout = 25000L) }.getOrNull()
			?: return emptyList()
		val decoded = raw.trim().let { value ->
			if (value.length >= 2 && value.first() == '"' && value.last() == '"') {
				value.substring(1, value.length - 1).replace("\\\"", "\"")
			} else {
				value
			}
		}
		val array = runCatching { JSONArray(decoded) }.getOrNull() ?: return emptyList()
		return (0 until array.length()).mapNotNull { i ->
			val url = array.optString(i).takeIf { it.isNotBlank() && !isTrapAsset(it) } ?: return@mapNotNull null
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}

	private fun buildYmMediaToken(imageUrl: String): String {
		val date = ymReqDateFormat.format(Date())
		val raw = "yq-${imageUrl.take(32)}-$date"
		return context.encodeBase64(raw.toByteArray(Charsets.UTF_8))
			.replace('+', '-')
			.replace('/', '_')
			.replace("=", "")
			.take(22)
	}

	private suspend fun fetchRawHtml(url: String): String =
		webClient.httpGet(url, getRequestHeaders()).parseRaw()

	private suspend fun fetchPagesFromApi(chapterUrl: String): List<MangaPage> {
		val chapterId = chapterUrl.substringAfter("id=", "").substringBefore('&').takeIf { it.isNotBlank() }
			?: throw ParseException("ID de capítulo inválido", chapterUrl)
		val json = webClient.httpGet(
			chapterUrl.toAbsoluteUrl(domain),
			getChapterApiHeaders(chapterId),
		).parseJson()
		json.optString("error").takeUnless { it.isBlank() || it.equals("null", true) }?.let { message ->
			if (message.contains("fingerprint", ignoreCase = true) || message.contains("Forbidden", ignoreCase = true)) {
				throw ParseException(message, chapterUrl)
			}
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
		val rawHtml = runCatching { fetchRawHtml("https://$domain/ler/$slug/$chapterNumber") }.getOrNull()
			?: return emptyList()
		parsePagesFromReaderHtml(rawHtml).takeIf { it.isNotEmpty() }?.let { return it }
		extractChapterIdFromReader(rawHtml, chapterNumber)?.let { chapterId ->
			tryFetchPagesFromApi("/api/chapters?id=$chapterId").takeIf { it.isNotEmpty() }?.let { return it }
		}
		return emptyList()
	}

	private fun parsePagesFromReaderHtml(rawHtml: String): List<MangaPage> {
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

	private fun extractChapterIdFromReader(html: String, chapterNumber: String): String? {
		val sources = listOf(html, extractRscPayload(html))
		for (source in sources) {
			Regex("""chapterId\\?":\\?"([^\\"]+)""")
				.find(source)
				?.groupValues
				?.get(1)
				?.takeUnless { isTrapChapterId(it) }
				?.let { return it }
			for (pattern in READER_CHAPTER_ID_PATTERNS) {
				val regex = Regex(pattern.replace("%NUM%", Regex.escape(chapterNumber)))
				regex.find(source)?.groupValues?.get(1)?.let { id ->
					if (!isTrapChapterId(id)) {
						return id
					}
				}
			}
		}
		return null
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

	private suspend fun fetchLibraryJson(page: Int, order: SortOrder, filter: MangaListFilter): JSONObject {
		val params = buildLibraryParams(page, order, filter)
		return runCatching {
			webClient.httpGet("https://$domain/api/library-proxy?$params", getApiHeaders()).parseJson()
		}.recoverCatching {
			webClient.httpGet("https://$domain/api/library?$params", getApiHeaders()).parseJson()
		}.getOrElse { JSONObject() }
	}

	private fun buildLibraryParams(page: Int, order: SortOrder, filter: MangaListFilter): String = buildString {
		append("page=")
		append(page.toString())
		append("&limit=")
		append(pageSize.toString())
		append("&sort=")
		append(
			when (order) {
				SortOrder.UPDATED -> "recent"
				SortOrder.POPULARITY -> "popular"
				SortOrder.ALPHABETICAL -> "alphabetical"
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
					MangaState.ONGOING -> "ONGOING"
					MangaState.FINISHED -> "COMPLETED"
					MangaState.PAUSED -> "HIATUS"
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

	private fun chapterApiId(chapterUrl: String): String? {
		val fromQuery = chapterUrl.substringAfter("?id=", "")
			.substringBefore('&')
			.takeIf { chapterUrl.contains("?id=") && it.isNotBlank() }
		if (!fromQuery.isNullOrBlank()) {
			return fromQuery
		}
		if (chapterUrl.startsWith("/api/chapters")) {
			return chapterUrl.substringAfter("id=", "").substringBefore('&').takeIf { it.isNotBlank() }
		}
		return null
	}

	private suspend fun fetchObraFromLibrary(slug: String, title: String? = null): JSONObject? {
		val queries = linkedSetOf(title?.trim(), slug.replace('-', ' '), slug)
			.filter { !it.isNullOrBlank() }
			.filterNotNull()
		for (query in queries) {
			val encoded = query.urlEncoded().replace("+", "%20")
			val json = runCatching {
				webClient.httpGet(
					"https://$domain/api/library-proxy?search=$encoded&limit=20",
					getApiHeaders(),
				).parseJson()
			}.recoverCatching {
				webClient.httpGet(
					"https://$domain/api/library?search=$encoded&limit=20",
					getApiHeaders(),
				).parseJson()
			}.getOrNull() ?: continue
			findObraInLibraryResponse(json, slug)?.let { return it }
		}
		return null
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

	private fun applyChapterIds(
		chapters: List<MangaChapter>,
		idMap: Map<String, String>,
	): List<MangaChapter> {
		if (idMap.isEmpty()) return chapters
		return chapters.map { chapter ->
			val id = idMap[chapterNumberKey(chapter.number)]
				?: chapter.number.toInt().takeIf { it.toFloat() == chapter.number }?.toString()?.let(idMap::get)
			if (id.isNullOrBlank() || chapter.url.startsWith("/api/chapters")) {
				chapter
			} else {
				chapter.copy(url = "${chapter.url.substringBefore('?')}?id=$id")
			}
		}
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

	private fun mapRecentLibraryChapters(slug: String, recentChapters: JSONArray): List<MangaChapter> {
		val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", sourceLocale)
		return (0 until recentChapters.length()).mapNotNull { index ->
			val recent = recentChapters.optJSONObject(index) ?: return@mapNotNull null
			val number = recent.optString("number").toFloatOrNull() ?: return@mapNotNull null
			val id = recent.getStringOrNull("id")?.takeUnless { isTrapChapterId(it) }
			val numberKey = chapterNumberKey(number)
			val chapterUrl = if (id != null) {
				"/ler/$slug/$numberKey?id=$id"
			} else {
				"/ler/$slug/$numberKey"
			}
			MangaChapter(
				id = generateUid(chapterUrl),
				title = recent.getStringOrNull("chapterSlug")?.replace('-', ' ')?.replaceFirstChar {
					if (it.isLowerCase()) it.titlecase(sourceLocale) else it.toString()
				},
				number = number,
				volume = 0,
				url = chapterUrl,
				scanlator = null,
				uploadDate = recent.getStringOrNull("releaseAt")
					?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() } ?: 0L,
				branch = null,
				source = source,
			)
		}
	}

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
			val chapterUrl = if (known != null) {
				"/ler/$slug/$numberKey?id=$known"
			} else {
				"/ler/$slug/$numberKey"
			}
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
		val selector = """a[href^="/ler/$slug/"]"""
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", sourceLocale)
		val anchors = runCatching { doc.select(selector) }.getOrNull().orEmpty()
		return anchors.mapNotNull { anchor ->
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

	private fun parseChaptersFromHtmlText(html: String, slug: String): List<MangaChapter> {
		val regex = Regex("""href="(/ler/${Regex.escape(slug)}/(\d+(?:\.\d+)?))"""")
		return regex.findAll(html).mapNotNull { match ->
			val href = match.groupValues[1]
			val number = match.groupValues[2].toFloatOrNull() ?: return@mapNotNull null
			MangaChapter(
				id = generateUid(href),
				title = null,
				number = number,
				volume = 0,
				url = href,
				scanlator = null,
				uploadDate = 0L,
				branch = null,
				source = source,
			)
		}.distinctBy { it.url }.toList()
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

	private val tagsCache = suspendLazy(initializer = ::loadTags)

	private suspend fun fetchTags(): Set<MangaTag> = tagsCache.get()

	private suspend fun loadTags(): Set<MangaTag> {
		val fromTaurus = runCatching {
			webClient.httpGet("$PUBLIC_API/genres", getTaurusHeaders()).parseJsonArray()
		}.getOrNull()
		if (fromTaurus != null && fromTaurus.length() > 0) {
			return parseGenreArray(fromTaurus)
		}
		val arr = runCatching {
			webClient.httpGet("https://$domain/api/genres", getApiHeaders()).parseJsonArray()
		}.getOrNull() ?: return emptySet()
		return parseGenreArray(arr)
	}

	private fun parseGenreArray(arr: JSONArray): Set<MangaTag> {
		val result = ArraySet<MangaTag>(arr.length())
		for (i in 0 until arr.length()) {
			val name = when (val item = arr.opt(i)) {
				is JSONObject -> item.optString("name").ifBlank { item.optString("title") }
				else -> arr.optString(i)
			}.takeIf { it.isNotBlank() && it != "null" } ?: continue
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
		optJSONArray("garimpo")
			?: optJSONArray("prateleira")
			?: optJSONArray("acervo")
			?: optJSONArray("obras")
			?: optJSONArray("data")
			?: optJSONArray("catalogo")
			?: optEncodedLibraryArray("garimpo")
			?: optEncodedLibraryArray("catalogo")

	private fun org.json.JSONObject.optEncodedLibraryArray(key: String): org.json.JSONArray? {
		val encoded = optString(key).takeUnless { it.isBlank() } ?: return null
		val decrypted = runCatching {
			CryptoAES(context).decrypt(encoded, LIBRARY_AES_PASSWORD)
		}.getOrNull() ?: runCatching {
			context.decodeBase64(encoded).toString(Charsets.UTF_8)
		}.getOrNull() ?: return null
		return runCatching { org.json.JSONArray(decrypted) }.getOrNull()
	}

	private companion object {
		private const val ACCEPT_LANGUAGE = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7"
		private const val PUBLIC_API = "https://yomu.tauruus.com"
		private const val LIBRARY_AES_PASSWORD = "yomu_trolling_scrapers_v1"
		private val ymReqDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
			timeZone = TimeZone.getTimeZone("UTC")
		}
		val READER_CHAPTER_ID_PATTERNS = listOf(
			"""\\"chapterId\\":\\"([^\\]+)\\"""",
			"""\"chapterId\":\"([^\"]+)\"""",
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
