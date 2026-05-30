package org.koitharu.kotatsu.parsers.site.pt

import androidx.collection.ArraySet
import okhttp3.Headers
import org.jsoup.nodes.Document
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.core.PagedMangaParser
import org.koitharu.kotatsu.parsers.exception.ParseException
import org.koitharu.kotatsu.parsers.model.*
import org.koitharu.kotatsu.parsers.util.*
import org.koitharu.kotatsu.parsers.util.json.getDoubleOrDefault
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import org.koitharu.kotatsu.parsers.util.json.mapJSON
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNull
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
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

	override fun getRequestHeaders(): Headers = super.getRequestHeaders().newBuilder()
		.add("Origin", "https://$domain")
		.add("Referer", "https://$domain/")
		.build()

	private fun getApiHeaders(): Headers = getRequestHeaders().newBuilder()
		.set("Accept", "application/json")
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
		return data.mapJSON { obj ->
			val slug = obj.getString("slug")
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
				coverUrl = obj.getStringOrNull("cover"),
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
		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain), getRequestHeaders()).parseHtml()
		val slug = manga.url.removePrefix("/obra/").trimEnd('/')
		val html = doc.html()
		val rsc = extractRscPayload(html)

		val description = doc.selectFirst("meta[property=og:description]")?.attr("content")
			?.takeUnless { it.isBlank() }
			?: extractJsonString(rsc, "description")
		val author = extractJsonString(rsc, "author")
		val artist = extractJsonString(rsc, "artist")
		val coverImage = doc.selectFirst("meta[property=og:image]")?.attr("content")
			?: extractJsonString(rsc, "coverImage")

		val chapters = parseChaptersFromRsc(rsc)
			.ifEmpty { parseChaptersFromHtml(doc, slug) }
			.sortedBy { it.number }

		val authors = buildSet {
			author?.takeUnless { it.isBlank() }?.let(::add)
			artist?.takeUnless { it.isBlank() || it == author }?.let(::add)
		}

		return manga.copy(
			description = description,
			authors = authors,
			coverUrl = coverImage ?: manga.coverUrl,
			largeCoverUrl = coverImage ?: manga.largeCoverUrl,
			chapters = chapters,
		)
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		if (chapter.url.startsWith("/api/chapters")) {
			return fetchPagesFromApi(chapter.url)
		}
		val match = CHAPTER_URL_REGEX.matchEntire(chapter.url)
			?: throw ParseException("Formato de URL de capítulo inválido", chapter.url)
		val slug = resolveObraSlug(match.groupValues[1])
		val chapterNumber = match.groupValues[2]
		val chapterId = resolveChapterId(slug, chapterNumber)
		if (chapterId != null) {
			val pages = fetchPagesFromApi("/api/chapters?id=$chapterId")
			if (pages.isNotEmpty()) {
				return pages
			}
		}
		val readerPages = fetchPagesFromReader(slug, chapterNumber)
		if (readerPages.isNotEmpty()) {
			return readerPages
		}
		throw ParseException("Não foi possível carregar as páginas do capítulo", chapter.url)
	}

	override suspend fun getPageUrl(page: MangaPage): String {
		if (page.url.startsWith("http://") || page.url.startsWith("https://")) {
			return page.url
		}
		return super.getPageUrl(page)
	}

	private suspend fun fetchPagesFromApi(chapterUrl: String): List<MangaPage> {
		val json = webClient.httpGet(chapterUrl.toAbsoluteUrl(domain), getApiHeaders()).parseJson()
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
			result.add(
				MangaPage(
					id = generateUid(url),
					url = url,
					preview = null,
					source = source,
				),
			)
		}
		return result
	}

	private suspend fun fetchPagesFromReader(slug: String, chapterNumber: String): List<MangaPage> {
		val readerUrl = "https://$domain/ler/$slug/$chapterNumber"
		val rawHtml = webClient.httpGet(readerUrl, getRequestHeaders()).parseHtml().html()
		val rsc = extractRscPayload(rawHtml)
		val urls = LinkedHashSet<String>()
		CDN_PAGE_REGEX.findAll(rawHtml).forEach { urls.add(it.value) }
		CDN_PAGE_REGEX.findAll(rsc).forEach { urls.add(it.value) }
		IMAGENS_LISTA_REGEX.find(rsc)?.groupValues?.get(1)?.let { listBody ->
			Regex("""https://[^"\\]+""").findAll(listBody).forEach { urls.add(it.value) }
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
			"https://$domain/api/library?slug=$slug&limit=1",
			getApiHeaders(),
		).parseJson()
		val obra = json.optLibraryArray()?.optJSONObject(0) ?: return null
		val recent = obra.optJSONArray("recentChapters") ?: return null
		for (i in 0 until recent.length()) {
			val ch = recent.optJSONObject(i) ?: continue
			if (ch.optString("number") == chapterNumber) {
				return ch.getStringOrNull("id")
			}
		}
		return null
	}

	private suspend fun getChapterIdMap(slug: String): Map<String, String> {
		chapterIdCache[slug]?.let { return it }
		val response = webClient.httpGet("https://$domain/obra/$slug", getRequestHeaders())
		if (!response.isSuccessful) {
			response.close()
			chapterIdCache[slug] = emptyMap()
			return emptyMap()
		}
		val doc = response.parseHtml()
		val rsc = extractRscPayload(doc.html())
		val map = parseChapterIdMapFromRsc(rsc).toMutableMap()
		parseChaptersFromRsc(rsc).forEach { ch ->
			val numKey = chapterNumberKey(ch.number)
			val id = ch.url.removePrefix("/api/chapters?id=")
			if (id.isNotBlank()) {
				map.putIfAbsent(numKey, id)
			}
		}
		chapterIdCache[slug] = map
		return map
	}

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
			?: optJSONArray("catalogo")
			?: optJSONArray("obras")
			?: optJSONArray("data")

	private companion object {
		val CHAPTER_URL_REGEX = Regex("""^/ler/([^/]+)/([^/]+)$""")
		val CHAPTER_ENTRY_REGEX = Regex("""\{"number":(\d+(?:\.\d+)?).*?"id":"([^"]+)"""")
		val IMAGENS_LISTA_REGEX = Regex(""""imagens_lista":\[([^\]]*)\]""")
		val CDN_PAGE_REGEX = Regex(
			"""https://cdn\.(?:monstercomics|yomu)\.com\.br/obras/[^"'\s<>\\]+\.(?:webp|jpg|jpeg|png)""",
		)
	}
}
