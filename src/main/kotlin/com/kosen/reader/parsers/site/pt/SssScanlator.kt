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
import com.kosen.reader.parsers.MangaParserAuthProvider
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.core.PagedMangaParser
import com.kosen.reader.parsers.exception.AuthRequiredException
import com.kosen.reader.parsers.exception.ParseException
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.util.*
import com.kosen.reader.parsers.util.getCookies
import com.kosen.reader.parsers.util.json.getDoubleOrDefault
import com.kosen.reader.parsers.util.json.getStringOrNull
import com.kosen.reader.parsers.util.json.mapJSON
import com.kosen.reader.parsers.util.json.mapJSONNotNull
import com.kosen.reader.parsers.util.suspendlazy.suspendLazy
import java.text.SimpleDateFormat
import java.util.*

@MangaSourceParser("SSSSCANLATOR", "Yomu", "pt")
internal class SssScanlator(context: MangaLoaderContext) :
	PagedMangaParser(context, MangaParserSource.SSSSCANLATOR, pageSize = 30),
	MangaParserAuthProvider {

	override val configKeyDomain = ConfigKey.Domain("yomu.com.br")

	override val sourceLocale: Locale = Locale("pt", "BR")

	override val authUrl: String
		get() = "https://$domain/login"

	private val authSessionKey = ConfigKey.AuthSession(defaultValue = false)

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(authSessionKey)
	}

	override val userAgentKey: ConfigKey.UserAgent = ConfigKey.UserAgent(
		"Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.135 Mobile Safari/537.36",
	)

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("User-Agent", config[userAgentKey])
		.add("Origin", "https://$domain")
		.add("Referer", "https://$domain/")
		.add("Accept-Language", ACCEPT_LANGUAGE)
		.build()

	private fun getApiHeaders(): Headers = getRequestHeaders().newBuilder()
		.set("Accept", "application/json")
		.add("x-yomu-client", "true")
		.add("Sec-Fetch-Dest", "empty")
		.add("Sec-Fetch-Mode", "cors")
		.add("Sec-Fetch-Site", "same-origin")
		.build()

	private fun getChapterApiHeaders(chapterId: String): Headers = getApiHeaders().newBuilder()
		.add("x-ym-req", buildYmReqToken(chapterId))
		.build()

	override suspend fun isAuthorized(): Boolean {
		if (hasSessionCookie()) return true
		if (fetchAuthSession()?.optJSONObject("user") != null) return true
		return config[authSessionKey]
	}

	override suspend fun getUsername(): String {
		val user = fetchAuthSession()?.optJSONObject("user") ?: throw AuthRequiredException(source)
		return user.getStringOrNull("name")
			?: user.getStringOrNull("email")
			?: user.getStringOrNull("username")
			?: "Yomu"
	}

	private fun hasSessionCookie(): Boolean =
		context.cookieJar.getCookies(domain).any { cookie ->
			val name = cookie.name.lowercase()
			name.contains("session-token") ||
				name.contains("session_token") ||
				name.endsWith("session-token") ||
				(name.contains("authjs") && name.contains("session"))
		}

	private suspend fun fetchAuthSession(): JSONObject? {
		val raw = runCatching {
			webClient.httpGet("https://$domain/api/auth/session", getApiHeaders()).parseRaw()
		}.getOrNull()?.trim() ?: return null
		if (raw.isEmpty() || raw == "null") return null
		return runCatching { JSONObject(raw) }.getOrNull()?.takeUnless { it.has("_xData") }
	}

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
		runCatching { fetchLibraryJson(page, order, filter) }.getOrNull()
			?.optLibraryArray()
			?.let(::mapLibraryArray)
			?.takeIf { it.isNotEmpty() }
			?.let { return it }
		fetchTaurusList(page, order, filter)?.takeIf { it.isNotEmpty() }?.let { return it }
		if (page == 1) {
			runCatching { fetchListViaJs(filter) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
			throw ParseException("Yomu não devolveu o catálogo. Puxe para atualizar.", "/")
		}
		return emptyList()
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val slug = manga.url.removePrefix("/obra/").substringBefore('?').trimEnd('/')
		val libraryObra = fetchObraFromLibrary(slug, manga.title)
		val lastNumber = libraryLastChapterNumber(libraryObra)
		val recentChapters = libraryObra?.optJSONArray("recentChapters")
		val recentList = recentChapters?.let { mapRecentLibraryChapters(slug, it) }.orEmpty()
		val apiChapters = fetchChaptersBySlug(slug, libraryObra?.getStringOrNull("id"))
		val needFullList = recentList.size < lastNumber.toInt().coerceAtLeast(1) &&
			(apiChapters?.size ?: 0) < lastNumber.toInt().coerceAtLeast(1)
		val jsManga = if (needFullList) {
			runCatching { fetchDetailsViaJs(slug, manga) }.getOrNull()
		} else {
			null
		}
		val pageUrl = manga.url.toAbsoluteUrl(domain)
		val html = if (needFullList && jsManga?.chapters.isNullOrEmpty()) {
			runCatching { fetchRawHtml(pageUrl) }.getOrNull()?.takeUnless { isFingerprintBlock(it) }
		} else {
			null
		}
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
		val chapters = mergeChapterList(
			slug = slug,
			parts = listOf(
				apiChapters.orEmpty(),
				jsManga?.chapters.orEmpty(),
				htmlChapters,
				rscChapters,
				recentList,
			),
			idMap = idMap,
			lastNumber = lastNumber,
		)

		if (chapters.isEmpty()) {
			jsManga?.takeIf { !it.chapters.isNullOrEmpty() }?.let { return it }
			fetchTaurusDetails(slug, manga)?.takeIf { !it.chapters.isNullOrEmpty() }?.let { return it }
			throw ParseException("Não foi possível carregar os capítulos desta obra. Tente de novo em alguns segundos.", manga.url)
		}

		val title = libraryObra?.getStringOrNull("title")?.takeIf { it.isNotBlank() }
			?: pageMeta?.title
			?: jsManga?.title
			?: manga.title
		val coverImage = libraryObra?.getStringOrNull("cover")?.takeUnless { isTrapAsset(it) }
			?: pageMeta?.coverImage
			?: jsManga?.coverUrl?.takeUnless { isTrapAsset(it) }
			?: manga.coverUrl?.takeUnless { isTrapAsset(it) }
		val description = if (isTrapPayload(rsc)) {
			jsManga?.description?.takeUnless { isTrapDescription(it) }
				?: manga.description?.takeUnless { isTrapDescription(it) }
		} else {
			doc?.selectFirst("meta[property=og:description]")?.attr("content")
				?.takeUnless { it.isBlank() || isTrapDescription(it) }
				?: extractJsonString(rsc, "description")?.takeUnless { isTrapDescription(it) }
				?: jsManga?.description?.takeUnless { isTrapDescription(it) }
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
		val chapterId = chapterApiId(chapter.url)
		if (!chapterId.isNullOrBlank()) {
			try {
				return fetchPagesFromApi("/api/chapters?id=$chapterId")
			} catch (e: ParseException) {
				if (isTerminalChapterError(e.shortMessage)) throw e
			} catch (_: Exception) {
				// API recusou o HTTP; tenta o mesmo ID pelo WebView.
			}
		}
		if (match != null) {
			runCatching { fetchPagesViaJs(match.groupValues[1], match.groupValues[2], chapterId) }
				.getOrNull()
				?.takeIf { it.isNotEmpty() }
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
		if (url.host.contains("yomu", ignoreCase = true) && request.header("x-yomu-client") == null) {
			builder.header("x-yomu-client", "true")
			changed = true
		}
		if (path.startsWith("/api/") && request.header("Sec-Fetch-Dest") == null) {
			builder.header("Sec-Fetch-Dest", "empty")
			builder.header("Sec-Fetch-Mode", "cors")
			builder.header("Sec-Fetch-Site", "same-origin")
			changed = true
		}
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
		val response = chain.proceed(if (changed) builder.build() else request)
		if (path.contains("/api/chapters") && response.code in 400..499) {
			val contentType = response.header("Content-Type").orEmpty()
			if (contentType.contains("json", ignoreCase = true)) {
				return response.newBuilder().code(200).message("OK").build()
			}
		}
		return response
	}

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
			parseLibraryManga(obj)
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

	private fun wrapEvaluateJs(asyncJs: String): String = buildString {
		append("window.__evaluateJsDone = undefined;\n")
		append("(function(){\n")
		append("function finish(value){\n")
		append("try {\n")
		append("window.__evaluateJsDone = (typeof value === 'string') ? value : JSON.stringify(value);\n")
		append("} catch (e) { window.__evaluateJsDone = '[]'; }\n")
		append("}\n")
		append("function onLoginWall(){\n")
		append("const p = (location.pathname || '/').replace(/\\/+$/, '') || '/';\n")
		append("return p === '/login' || p === '/registro';\n")
		append("}\n")
		append("function ymReq(id){\n")
		append("const n = new Date();\n")
		append("const date = n.getUTCFullYear().toString() + String(n.getUTCMonth()+1).padStart(2,'0') + String(n.getUTCDate()).padStart(2,'0');\n")
		append("const raw = 'yk-v3-' + id + '-' + date;\n")
		append("try { return btoa(raw).replace(/\\+/g,'-').replace(/\\//g,'_').replace(/=/g,'').slice(0,24); } catch (e) { return ''; }\n")
		append("}\n")
		append("async function fetchJson(url){\n")
		append("try {\n")
		append("const r = await fetch(url, {credentials:'include', cache:'no-store', headers:{'Accept':'application/json','x-yomu-client':'true'}});\n")
		append("if (!r.ok) return null;\n")
		append("return await r.json();\n")
		append("} catch (e) { return null; }\n")
		append("}\n")
		append("async function fetchChapter(id){\n")
		append("try {\n")
		append("const r = await fetch('/api/chapters?id=' + encodeURIComponent(id), {credentials:'include', cache:'no-store', headers:{'Accept':'application/json','x-yomu-client':'true','x-ym-req': ymReq(id)}});\n")
		append("if (!r.ok) return null;\n")
		append("return await r.json();\n")
		append("} catch (e) { return null; }\n")
		append("}\n")
		append("function pagesFromPayload(data){\n")
		append("if (!data || data.error || data._xData) return null;\n")
		append("const images = (data.chapter && (data.chapter.content || data.chapter.images)) || data.images || data.content || data.pages;\n")
		append("if (!Array.isArray(images) || !images.length) return null;\n")
		append("const urls = images.map(function(x){\n")
		append("if (typeof x === 'string') return x;\n")
		append("if (!x) return '';\n")
		append("return x.url || x.src || x.image || x.path || '';\n")
		append("}).filter(Boolean);\n")
		append("return urls.length ? urls : null;\n")
		append("}\n")
		append("Promise.resolve().then(async function(){\n")
		append(asyncJs)
		append("\n}).catch(function(){ finish('[]'); });\n")
		append("})();")
	}

	private fun unwrapEvaluateJs(raw: String): String {
		var value = raw.trim()
		repeat(2) {
			if (value.length >= 2 && value.first() == '"' && value.last() == '"') {
				value = value.substring(1, value.length - 1)
					.replace("\\\\", "\\")
					.replace("\\\"", "\"")
					.replace("\\n", "\n")
			}
		}
		return value
	}

	private fun parseJsValue(raw: String): Any? {
		val decoded = unwrapEvaluateJs(raw)
		if (decoded.isEmpty() || decoded == "null" || decoded == "undefined") return null
		return runCatching { JSONArray(decoded) }.getOrNull()
			?: runCatching { JSONObject(decoded) }.getOrNull()
	}

	private fun isAuthWall(value: Any?): Boolean =
		(value as? JSONObject)?.optString("error") == "auth"

	private fun isFingerprintBlock(text: String): Boolean =
		text.contains("Invalid browser fingerprint", ignoreCase = true) ||
			text.contains("Forbidden - Invalid", ignoreCase = true)

	private suspend fun evalYomuJs(pageUrl: String, asyncJs: String, timeout: Long = 16000L): String? =
		runCatching {
			context.evaluateJs(
				pageUrl,
				wrapEvaluateJs(asyncJs),
				timeout = timeout,
				userAgent = config[userAgentKey],
			)
		}.getOrNull()

	private fun catalogTitle(raw: String?, slug: String): String {
		val title = raw?.trim().orEmpty()
		if (title.isNotEmpty() && !NUMERIC_TITLE_REGEX.matches(title)) return title
		return slug.replace('-', ' ')
	}

	private fun mapLibraryArray(data: JSONArray): List<Manga> =
		data.mapJSONNotNull { obj -> parseLibraryManga(obj) }

	private fun parseLibraryManga(obj: JSONObject): Manga? {
		if (isTrapLibraryItem(obj)) return null
		val slug = obj.getStringOrNull("slug")?.takeIf { it.isNotBlank() } ?: return null
		if (slug == "bloqueado") return null
		val relUrl = "/obra/$slug"
		return Manga(
			id = generateUid(relUrl),
			title = catalogTitle(obj.getStringOrNull("title"), slug),
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

	private fun toPages(array: JSONArray): List<MangaPage> =
		(0 until array.length()).mapNotNull { i ->
			val url = array.optString(i).takeIf { it.isNotBlank() && isChapterPageUrl(it) } ?: return@mapNotNull null
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}

	private suspend fun fetchListViaJs(filter: MangaListFilter): List<Manga>? {
		val query = JSONObject.quote(filter.query?.trim().orEmpty())
		val script = """
			const query = $query;
			const isNumericTitle = (t) => /^\d+([.,]\d+)?$/.test((t || '').trim());
			const pickTitle = (a, img, slug) => {
				const heading = a.querySelector('h2, h3, h4, [class*="title"], [class*="name"]');
				const candidates = [heading && heading.textContent, img && img.getAttribute('alt'), a.getAttribute('title'), a.getAttribute('aria-label')];
				for (const c of candidates) {
					const t = (c || '').trim();
					if (t && !isNumericTitle(t) && t.length < 120) return t;
				}
				return slug.replace(/-/g, ' ');
			};
			const collect = () => {
				const items = [];
				const seen = new Set();
				for (const a of document.querySelectorAll('a[href*="/obra/"]')) {
					const href = (a.getAttribute('href') || '').split('?')[0];
					const match = href.match(/\/obra\/([^/#]+)/);
					if (!match || match[1] === 'bloqueado' || /^\d+$/.test(match[1]) || seen.has(match[1])) continue;
					seen.add(match[1]);
					const img = a.querySelector('img');
					let cover = img?.getAttribute('src') || img?.getAttribute('data-src') || '';
					if (cover.indexOf('/_next/image') >= 0) {
						try { cover = decodeURIComponent(new URL(cover, location.origin).searchParams.get('url') || cover); } catch (e) {}
					}
					items.push({ slug: match[1], title: pickTitle(a, img, match[1]), cover: cover });
				}
				return items;
			};
			const libUrl = '/api/library-proxy?page=1&limit=30&sort=updated' + (query ? '&search=' + encodeURIComponent(query) : '');
			const libFirst = await fetchJson(libUrl);
			if (libFirst && !libFirst._xData && !libFirst.error) { finish(libFirst); return; }
			for (let attempts = 0; attempts < 20; attempts++) {
				if (onLoginWall()) { finish({error:'auth'}); return; }
				const items = collect();
				if (items.length > 0) { finish(items); return; }
				window.scrollTo(0, document.body.scrollHeight);
				await new Promise(r => setTimeout(r, 200));
			}
			finish([]);
		""".trimIndent()
		val pageUrl = if (!filter.query.isNullOrBlank()) {
			"https://$domain/?search=${filter.query.urlEncoded().replace("+", "%20")}"
		} else {
			"https://$domain/"
		}
		val raw = evalYomuJs(pageUrl, script, timeout = 14000L) ?: return null
		val value = parseJsValue(raw)
		if (isAuthWall(value)) return null
		val found = ArrayList<Manga>()
		fun walk(node: Any?, depth: Int) {
			if (node == null || depth > 5) return
			when (node) {
				is JSONArray -> {
					val mapped = node.mapJSONNotNull { obj -> parseLibraryManga(obj) }
					if (mapped.isNotEmpty()) found.addAll(mapped) else {
						for (i in 0 until node.length()) walk(node.opt(i), depth + 1)
					}
				}
				is JSONObject -> {
					if (node.has("_xData") || node.has("error")) return
					node.optLibraryArray()?.let { walk(it, depth + 1) }
					val keys = node.keys()
					while (keys.hasNext()) walk(node.opt(keys.next()), depth + 1)
				}
			}
		}
		walk(value, 0)
		return found.distinctBy { it.url }.takeIf { it.isNotEmpty() }
	}

	private suspend fun fetchDetailsViaJs(slug: String, manga: Manga): Manga? {
		val slugJson = JSONObject.quote(slug)
		val script = """
			const slug = $slugJson;
			if (onLoginWall()) { finish({error:'auth'}); return; }
			const toEntries = (arr) => {
				if (!Array.isArray(arr)) return [];
				return arr.map((ch) => {
					if (!ch) return null;
					const number = ch.number ?? ch.num ?? ch.chapterNumber;
					const id = ch.id || ch.chapterId;
					if (number == null || !id) return null;
					return { number: String(number), id: String(id) };
				}).filter(Boolean);
			};
			const fromApi = async () => {
				const urls = [
					'/api/library?slug=' + encodeURIComponent(slug),
					'/api/library/chapters?slug=' + encodeURIComponent(slug),
				];
				for (const url of urls) {
					const data = await fetchJson(url);
					if (!data || data.error || data._xData) continue;
					const arr = data.chapters || data.allChapters || data.capitulos || data.data;
					const entries = toEntries(arr);
					if (entries.length) return { title: data.title || '', entries: entries, links: [], total: entries.length };
				}
				return null;
			};
			const collect = () => {
				const links = [];
				const seen = new Set();
				for (const a of document.querySelectorAll('a[href*="/ler/"]')) {
					const href = (a.getAttribute('href') || '').split('#')[0];
					if (!href || seen.has(href)) continue;
					seen.add(href);
					links.push(href);
				}
				const html = document.documentElement ? document.documentElement.innerHTML : '';
				const entries = [];
				const seenId = new Set();
				const re = /"number"\s*:\s*"?(\d+(?:\.\d+)?)"?[\s\S]{0,280}?"id"\s*:\s*"([0-9a-f-]{8,})"/gi;
				let m;
				while ((m = re.exec(html))) {
					if (seenId.has(m[2])) continue;
					seenId.add(m[2]);
					entries.push({ number: m[1], id: m[2] });
				}
				const allMatch = html.match(/"allChapters"\s*:\s*(\[[\s\S]*?\])/);
				if (allMatch) {
					try { toEntries(JSON.parse(allMatch[1])).forEach((e) => { if (!seenId.has(e.id)) { seenId.add(e.id); entries.push(e); } }); } catch (e) {}
				}
				const totalMatch = html.match(/chapterTotal["']?\s*[:=]\s*(\d+)/) || html.match(/"totalChapters"\s*:\s*(\d+)/);
				const total = totalMatch ? parseInt(totalMatch[1], 10) : entries.length;
				return {
					title: document.querySelector('h1')?.textContent?.trim() || '',
					description: document.querySelector('meta[property="og:description"]')?.getAttribute('content') || '',
					cover: document.querySelector('meta[property="og:image"]')?.getAttribute('content') || '',
					links: links,
					entries: entries,
					total: total,
				};
			};
			const api = await fromApi();
			if (api && api.entries.length > 2) { finish(api); return; }
			let best = collect();
			for (let attempts = 0; attempts < 12; attempts++) {
				const value = collect();
				if (value.links.length > best.links.length) best.links = value.links;
				if (value.entries.length > best.entries.length) best.entries = value.entries;
				if (value.total > best.total) best.total = value.total;
				if (value.title) best.title = value.title;
				if (best.entries.length > 5 || best.links.length > 5) {
					if (attempts >= 2) { finish(best); return; }
				}
				window.scrollTo(0, document.body.scrollHeight);
				await new Promise(r => setTimeout(r, 220));
			}
			if (api && api.entries.length > best.entries.length) best.entries = api.entries;
			finish(best);
		""".trimIndent()
		val raw = evalYomuJs("https://$domain/obra/$slug", script, timeout = 14000L) ?: return null
		val json = parseJsValue(raw) as? JSONObject ?: return null
		if (isAuthWall(json)) return null
		val found = LinkedHashMap<String, MangaChapter>()
		fun add(number: Float, url: String) {
			found.putIfAbsent(
				chapterNumberKey(number),
				MangaChapter(
					id = generateUid(url),
					title = null,
					number = number,
					volume = 0,
					url = url,
					scanlator = null,
					uploadDate = 0L,
					branch = null,
					source = source,
				),
			)
		}
		json.optJSONArray("entries")?.let { entries ->
			for (i in 0 until entries.length()) {
				val obj = entries.optJSONObject(i) ?: continue
				val number = obj.optString("number").toFloatOrNull() ?: continue
				val id = obj.optString("id").takeUnless { it.isBlank() || isTrapChapterId(it) } ?: continue
				val key = chapterNumberKey(number)
				add(number, "/ler/$slug/$key?id=$id")
			}
		}
		json.optJSONArray("links")?.let { links ->
			for (i in 0 until links.length()) {
				val rawHref = links.optString(i).trim().substringBefore('#')
				val path = rawHref.substringAfter("/ler/", "").substringBefore('?')
				if (path.isEmpty()) continue
				val number = path.substringAfterLast('/').toFloatOrNull() ?: continue
				val query = rawHref.substringAfter('?', "")
				val id = query.substringAfter("id=", "").substringBefore('&').takeIf { it.isNotBlank() }
					?.takeUnless { isTrapChapterId(it) } ?: continue
				val key = chapterNumberKey(number)
				add(number, "/ler/$slug/$key?id=$id")
			}
		}
		val chapters = found.values.sortedBy { it.number }
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

	private suspend fun fetchPagesViaJs(slug: String, chapterNumber: String, knownId: String? = null): List<MangaPage> {
		val knownIdJson = JSONObject.quote(knownId.orEmpty())
		val script = """
			const knownId = $knownIdJson;
			if (onLoginWall()) { finish({error:'auth'}); return; }
			const tryId = async (id) => {
				if (!id) return null;
				return pagesFromPayload(await fetchChapter(id));
			};
			if (knownId) {
				const images = await tryId(knownId);
				if (images) { finish(images); return; }
			}
			for (let attempts = 0; attempts < 20; attempts++) {
				const params = new URLSearchParams(location.search || '');
				let id = params.get('id');
				if (!id && location.pathname && location.pathname.indexOf('/ler/') >= 0) {
					const html = document.documentElement ? document.documentElement.innerHTML : '';
					const m = html.match(/"chapterId"\s*:\s*"([^"]+)"/) || html.match(/chapterId["']?\s*[:=]\s*["']([^"']+)/);
					if (m) id = m[1];
				}
				const images = await tryId(id);
				if (images) { finish(images); return; }
				await new Promise(r => setTimeout(r, 250));
			}
			finish([]);
		""".trimIndent()
		val pageUrl = if (knownId.isNullOrBlank()) {
			"https://$domain/ler/$slug/$chapterNumber"
		} else {
			"https://$domain/obra/$slug"
		}
		val raw = evalYomuJs(pageUrl, script, timeout = 16000L) ?: return emptyList()
		val value = parseJsValue(raw) ?: return emptyList()
		if (isAuthWall(value)) return emptyList()
		val array = when (value) {
			is JSONArray -> value
			is JSONObject -> value.optJSONArray("images") ?: return emptyList()
			else -> return emptyList()
		}
		return toPages(array)
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
		webClient.httpGet(
			url,
			getRequestHeaders().newBuilder()
				.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
				.set("Sec-Fetch-Dest", "document")
				.set("Sec-Fetch-Mode", "navigate")
				.set("Sec-Fetch-Site", "none")
				.set("Upgrade-Insecure-Requests", "1")
				.build(),
		).parseRaw()

	private suspend fun fetchPagesFromApi(chapterUrl: String): List<MangaPage> {
		val chapterId = chapterUrl.substringAfter("id=", "").substringBefore('&').takeIf { it.isNotBlank() }
			?: throw ParseException("ID de capítulo inválido", chapterUrl)
		val json = webClient.httpGet(
			chapterUrl.toAbsoluteUrl(domain),
			getChapterApiHeaders(chapterId),
		).parseJson()
		if (json.has("_xData")) {
			return emptyList()
		}
		json.optString("error").takeUnless { it.isBlank() || it.equals("null", true) }?.let { message ->
			throw ParseException(chapterErrorMessage(message, json), chapterUrl)
		}
		if (json.optBoolean("isLocked") || json.optString("lockedType").equals("VIP", true)) {
			throw ParseException(chapterErrorMessage(json.optString("error"), json), chapterUrl)
		}
		val chapter = json.optJSONObject("chapter")
			?: throw ParseException("Resposta inválida da API de capítulos", chapterUrl)
		val content = chapter.optJSONArray("content")
			?: throw ParseException("Capítulo sem páginas", chapterUrl)
		val result = ArrayList<MangaPage>(content.length())
		for (i in 0 until content.length()) {
			val url = content.getString(i)
			if (!isChapterPageUrl(url)) {
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

	private fun chapterErrorMessage(raw: String?, json: JSONObject): String {
		val message = raw?.trim().orEmpty()
		val locked = json.optBoolean("isLocked") || json.optString("lockedType").equals("VIP", true)
		return when {
			locked || message.contains("VIP", ignoreCase = true) ->
				message.ifBlank { "Este capítulo é exclusivo para VIPs." }
			message.isNotBlank() -> message
			else -> "Não foi possível carregar as páginas do capítulo"
		}
	}

	private fun isTerminalChapterError(message: String?): Boolean {
		val text = message.orEmpty()
		return text.contains("VIP", ignoreCase = true) ||
			text.contains("bloqueado", ignoreCase = true) ||
			text.contains("exclusivo", ignoreCase = true) ||
			text.contains("não foi lançado", ignoreCase = true)
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
					SortOrder.UPDATED -> "updated"
					SortOrder.POPULARITY -> "popular"
					SortOrder.ALPHABETICAL -> "alphabetical"
					else -> "updated"
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

	private suspend fun fetchChaptersBySlug(slug: String, seriesId: String?): List<MangaChapter>? {
		val encodedSlug = slug.urlEncoded()
		val urls = buildList {
			add("https://$domain/api/library?slug=$encodedSlug")
			add("https://$domain/api/library/chapters?slug=$encodedSlug")
			if (!seriesId.isNullOrBlank()) {
				add("https://$domain/api/library?slug=$encodedSlug&id=$seriesId")
			}
		}
		for (url in urls) {
			val json = runCatching { webClient.httpGet(url, getApiHeaders()).parseJson() }.getOrNull() ?: continue
			if (json.has("error") || json.has("_xData")) continue
			val arr = json.optJSONArray("chapters")
				?: json.optJSONArray("allChapters")
				?: json.optJSONArray("capitulos")
				?: continue
			val mapped = mapGenericChapterArray(slug, arr)
			if (mapped.size >= 2) return mapped
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
			val id = recent.getStringOrNull("id")?.takeUnless { it.isBlank() || isTrapChapterId(it) }
				?: return@mapNotNull null
			val numberKey = chapterNumberKey(number)
			val chapterUrl = "/ler/$slug/$numberKey?id=$id"
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

	private fun mapGenericChapterArray(slug: String, chapters: JSONArray): List<MangaChapter> {
		return (0 until chapters.length()).mapNotNull { index ->
			val obj = chapters.optJSONObject(index) ?: return@mapNotNull null
			val number = obj.optString("number").toFloatOrNull()
				?: obj.optDouble("number", Double.NaN).takeIf { !it.isNaN() }?.toFloat()
				?: obj.optDouble("chapterNumber", Double.NaN).takeIf { !it.isNaN() }?.toFloat()
				?: return@mapNotNull null
			val id = (obj.getStringOrNull("id") ?: obj.getStringOrNull("chapterId"))
				?.takeUnless { it.isBlank() || isTrapChapterId(it) }
			val numberKey = chapterNumberKey(number)
			val chapterUrl = if (id != null) "/ler/$slug/$numberKey?id=$id" else "/ler/$slug/$numberKey"
			MangaChapter(
				id = generateUid(chapterUrl),
				title = obj.getStringOrNull("title") ?: obj.getStringOrNull("chapterSlug")?.replace('-', ' '),
				number = number,
				volume = 0,
				url = chapterUrl,
				scanlator = null,
				uploadDate = 0L,
				branch = null,
				source = source,
			)
		}
	}

	private fun libraryLastChapterNumber(obj: JSONObject?): Float {
		if (obj == null) return 0f
		val fromLast = when (val value = obj.opt("lastChapter")) {
			is Number -> value.toFloat()
			is String -> Regex("""(\d+(?:\.\d+)?)""").find(value)?.value?.toFloatOrNull() ?: 0f
			else -> 0f
		}
		var fromRecent = 0f
		obj.optJSONArray("recentChapters")?.let { arr ->
			for (i in 0 until arr.length()) {
				val number = arr.optJSONObject(i)?.optString("number")?.toFloatOrNull() ?: continue
				if (number > fromRecent) fromRecent = number
			}
		}
		return maxOf(fromLast, fromRecent)
	}

	private fun mergeChapterList(
		slug: String,
		parts: List<List<MangaChapter>>,
		idMap: Map<String, String>,
		lastNumber: Float,
	): List<MangaChapter> {
		val map = LinkedHashMap<String, MangaChapter>()
		fun put(chapter: MangaChapter) {
			if (isTrapChapter(chapter) || chapter.number <= 0f) return
			val key = chapterNumberKey(chapter.number)
			val existing = map[key]
			val newId = chapterApiId(chapter.url)
			val oldId = existing?.let { chapterApiId(it.url) }
			if (existing == null || (oldId.isNullOrBlank() && !newId.isNullOrBlank())) {
				map[key] = chapter
			}
		}
		for (list in parts) {
			list.forEach(::put)
		}
		applyChapterIds(map.values.toList(), idMap).forEach(::put)
		val lastInt = lastNumber.toInt()
		if (lastInt >= 1) {
			for (index in 1..lastInt) {
				val key = index.toString()
				if (map.containsKey(key)) continue
				val known = idMap[key]
				val chapterUrl = if (!known.isNullOrBlank()) {
					"/ler/$slug/$key?id=$known"
				} else {
					"/ler/$slug/$key"
				}
				put(
					MangaChapter(
						id = generateUid(chapterUrl),
						title = null,
						number = index.toFloat(),
						volume = 0,
						url = chapterUrl,
						scanlator = null,
						uploadDate = 0L,
						branch = null,
						source = source,
					),
				)
			}
		}
		return map.values.sortedBy { it.number }
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

	private fun isTrapLibraryItem(obj: JSONObject): Boolean {
		val slug = obj.getStringOrNull("slug").orEmpty()
		if (slug.isBlank() || slug == "bloqueado" || slug.all { it.isDigit() }) return true
		val type = obj.optString("type").lowercase(Locale.ROOT)
		if (type == "yaoi" || type == "yuri") return true
		return isTrapAsset(obj.optString("cover"))
	}

	private fun isChapterPageUrl(url: String): Boolean {
		if (isTrapAsset(url)) return false
		val lower = url.lowercase(Locale.ROOT)
		if (lower.contains("mascote") || lower.contains("/images/")) return false
		if (lower.contains("/capa/") || lower.contains("/cover") ||
			lower.contains("/poster") || lower.contains("/banner")
		) {
			return false
		}
		return lower.contains("/chapters/") ||
			lower.contains("secure-image") ||
			lower.contains("proxy-image")
	}

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
		val CHAPTER_LIST_ENTRY_REGEX = Regex(
			"""\\?"number\\?":(\d+(?:\.\d+)?)[^}]{0,500}?\\?"id\\?":\\?"([^\\]+)\\?"""",
		)
		val CHAPTER_URL_REGEX = Regex("""^/ler/([^/]+)/([^/]+)$""")
		val NUMERIC_TITLE_REGEX = Regex("""^\d+([.,]\d+)?$""")
		val CHAPTER_ENTRY_REGEX = Regex("""\{"number":(\d+(?:\.\d+)?).*?"id":"([^"]+)"""")
	}
}
