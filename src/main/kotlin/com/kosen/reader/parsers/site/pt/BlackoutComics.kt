package com.kosen.reader.parsers.site.pt

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Headers

import okhttp3.HttpUrl.Companion.toHttpUrl

import okhttp3.Interceptor

import okhttp3.Response

import org.jsoup.Jsoup

import org.jsoup.nodes.Document

import com.kosen.reader.parsers.MangaLoaderContext

import com.kosen.reader.parsers.MangaParserAuthProvider

import com.kosen.reader.parsers.MangaSourceParser

import com.kosen.reader.parsers.config.ConfigKey

import com.kosen.reader.parsers.core.PagedMangaParser

import com.kosen.reader.parsers.exception.AuthRequiredException

import com.kosen.reader.parsers.exception.ParseException

import com.kosen.reader.parsers.model.*

import com.kosen.reader.parsers.network.UserAgents

import com.kosen.reader.parsers.util.*
import com.kosen.reader.parsers.util.getCookies
import com.kosen.reader.parsers.util.json.mapJSON

import com.kosen.reader.parsers.webview.InterceptionConfig

import java.text.SimpleDateFormat

import java.util.*



@MangaSourceParser("BLACKOUT_COMICS", "Blackout Comics", "pt", ContentType.COMICS)

internal class BlackoutComics(context: MangaLoaderContext) :

	PagedMangaParser(context, MangaParserSource.BLACKOUT_COMICS, pageSize = 12),

	MangaParserAuthProvider {

	@Volatile
	private var cachedGenres: Set<MangaTag>? = null
	private val genresMutex = Mutex()

	override val configKeyDomain = ConfigKey.Domain("blackoutcomics.com")

	override val userAgentKey = ConfigKey.UserAgent(UserAgents.CHROME_MOBILE)

	private val authSessionKey = ConfigKey.AuthSession(defaultValue = false)

	override val sourceLocale: Locale = Locale("pt", "BR")

	override val authUrl: String
		get() = "https://$domain/entrar"

	override val authVerifyUrl: String
		get() = "https://$domain/comics/$AUTH_PROBE_COMIC_ID"

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(authSessionKey)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
	}

	override fun getRequestHeaders(): Headers = Headers.Builder()
		.add("User-Agent", config[userAgentKey])
		.add("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
		.build()

	override fun intercept(chain: Interceptor.Chain): Response {
		val userAgent = config[userAgentKey]
		val request = chain.request().newBuilder()
			.removeHeader("User-Agent")
			.header("User-Agent", userAgent)
			.build()
		return chain.proceed(request)
	}



	private fun getJsonHeaders(): Headers = getRequestHeaders().newBuilder()

		.set("Accept", "application/json, text/plain, */*")

		.set("Referer", "https://$domain/comics")

		.build()



	private fun getHtmlHeaders(): Headers = getRequestHeaders().newBuilder()

		.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

		.set("Referer", "https://$domain/")

		.build()



	override suspend fun isAuthorized(): Boolean {
		if (!hasAnySiteCookies()) {
			config[authSessionKey] = false
			return false
		}
		verifyLoginViaHttp()?.let { ok ->
			config[authSessionKey] = ok
			return ok
		}
		when (val verified = verifyLoginActive()) {
			true -> {
				config[authSessionKey] = true
				return true
			}
			false -> {
				config[authSessionKey] = false
				return false
			}
			null -> {
				config[authSessionKey] = false
				return false
			}
		}
	}

	private fun hasAnySiteCookies(): Boolean =
		context.cookieJar.getCookies(domain).isNotEmpty() ||
			context.cookieJar.getCookies("www.$domain").isNotEmpty()

	private suspend fun verifyLoginViaHttp(): Boolean? {
		if (!hasAnySiteCookies()) {
			return null
		}
		val homeDoc = runCatching {
			webClient.httpGet("https://$domain/", getHtmlHeaders()).parseHtml()
		}.getOrNull()
		if (homeDoc != null && isLoggedInDocument(homeDoc)) {
			return true
		}
		return probeAuthViaHttp()
	}

	private fun isLoggedInDocument(doc: Document): Boolean {
		when (doc.selectFirst("[data-auth='1'], [data-auth=\"1\"]") != null) {
			true -> return true
			false -> if (doc.selectFirst("[data-auth='0'], [data-auth=\"0\"]") != null) return false
		}
		return doc.select(
			"a[href*=sair], a[href*=logout], a[href*=perfil], a[href*=profile], " +
				"a[href*=minha-conta], .user-menu, .nav-user, " +
				"[data-user-name], .user-avatar, .header-user, .btn-user",
		).isNotEmpty()
	}

	private suspend fun probeAuthViaHttp(): Boolean? {
		val doc = runCatching {
			webClient.httpGet("https://$domain/comics/$AUTH_PROBE_COMIC_ID", getHtmlHeaders()).parseHtml()
		}.getOrNull() ?: return null
		val chapters = doc.select("#tab-capitulos-list li .chapter-link-wrap")
		if (chapters.isEmpty()) {
			return null
		}
		var locked = 0
		var readable = 0
		for (element in chapters) {
			val onclick = element.attr("onclick")
			if (onclick.contains("showLoginModal")) {
				locked++
			} else if (
				onclick.contains("location.href") ||
				element.selectFirst("a[href]") != null
			) {
				readable++
			}
		}
		return evaluateAuthProbeChapterState(locked, readable)
	}

	private fun evaluateAuthProbeChapterState(locked: Int, readable: Int): Boolean? = when {
		locked > 0 -> false
		readable > 0 -> true
		else -> null
	}

	private suspend fun evalJs(url: String, script: String, timeout: Long): String? =
		context.evaluateJs(url, script, timeout, config[userAgentKey])

	private val parserUserAgent: String
		get() = config[userAgentKey]

	override suspend fun getUsername(): String {
		if (!isAuthorized()) {
			throw AuthRequiredException(source)
		}
		val doc = runCatching {
			webClient.httpGet("https://$domain/", getHtmlHeaders()).parseHtml()
		}.getOrNull()
		return doc?.selectFirst(".user-name, .profile-name, .nav-user-name, [data-user-name]")
			?.text()?.trim()?.takeUnless { it.isBlank() }
			?: doc?.selectFirst("a[href*=perfil], a[href*=profile], a[href*=usuario]")
				?.text()?.trim()?.takeUnless { it.isBlank() }
			?: "Conta Blackout"
	}



	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = getOrCreateGenres(),
		availableStates = EnumSet.of(
			MangaState.ONGOING,
			MangaState.FINISHED,
		),
	)



	override val availableSortOrders: Set<SortOrder> = EnumSet.of(

		SortOrder.ALPHABETICAL,

		SortOrder.UPDATED,

	)



	override val filterCapabilities: MangaListFilterCapabilities

		get() = MangaListFilterCapabilities(

			isSearchSupported = true,
			isSearchWithFiltersSupported = true,

		)



	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {

		if (!filter.query.isNullOrEmpty()) {

			return searchComics(filter.query, filter)

		}

		if (order == SortOrder.UPDATED && filter.tags.isEmpty() && filter.states.isEmpty()) {
			return parseRecentListFromHtml(page)
		}

		val url = buildComicsListUrl(page, filter)

		parseJsonResponse(url)?.optJSONArray("items")?.let { items ->
			return items.mapJSON { parseMangaFromJson(it) }
		}

		return parseListFromHtml(page, filter)

	}

	private suspend fun parseRecentListFromHtml(page: Int): List<Manga> {
		val pageSuffix = if (page <= 1) "" else "?page=$page"
		val doc = webClient.httpGet("https://$domain/atualizados-recente$pageSuffix", getHtmlHeaders()).parseHtml()
		return doc.select("a.webtoon-card[href*=/comics/]").mapNotNull { anchor ->
			val href = anchor.attr("href").substringAfter("/comics/").trimEnd('/')
			val id = href.toIntOrNull() ?: return@mapNotNull null
			val title = anchor.selectFirst("img")?.attr("alt")?.takeUnless { it.isBlank() }
				?: anchor.selectFirst(".webtoon-title, .card-title, h3, h4")?.text()?.trim()?.takeUnless { it.isBlank() }
				?: return@mapNotNull null
			parseMangaFromJson(
				org.json.JSONObject()
					.put("PJT_ID", id)
					.put("PJT_NAME", title)
					.put("PJT_IMG_PR_URL", anchor.selectFirst("img")?.attr("src")?.toAbsoluteUrl(domain).orEmpty()),
			)
		}.distinctBy { it.id }
	}



	override suspend fun getDetails(manga: Manga): Manga {
		val projectId = manga.url.removePrefix("/comics/").trimEnd('/')
		val url = manga.url.toAbsoluteUrl(domain)

		var doc: Document
		var chapters: List<MangaChapter>
		if (config[authSessionKey]) {
			val webDoc = fetchProjectDocument(projectId, preferWebView = true)
			if (webDoc != null) {
				doc = webDoc
				chapters = parseChapters(doc, projectId)
			} else {
				doc = webClient.httpGet(url, getHtmlHeaders()).parseHtml()
				chapters = parseChapters(doc, projectId)
			}
		} else {
			doc = webClient.httpGet(url, getHtmlHeaders()).parseHtml()
			chapters = parseChapters(doc, projectId)
			if (chapters.isEmpty()) {
				fetchProjectDocument(projectId, preferWebView = false)?.let { webDoc ->
					val webChapters = parseChapters(webDoc, projectId)
					if (webChapters.isNotEmpty()) {
						doc = webDoc
						chapters = webChapters
					}
				}
			}
		}

		val description = doc.selectFirst("meta[name=description]")?.attr("content")

			?.takeUnless { it.isBlank() }

		val coverUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")

			?: doc.selectFirst(".project-cover")?.attr("src")

		val tags = doc.select(".genre-tag").mapTo(HashSet()) { el ->

			MangaTag(

				title = el.text(),

				key = el.text(),

				source = source,

			)

		}

		return manga.copy(

			description = description ?: manga.description,

			coverUrl = coverUrl ?: manga.coverUrl,

			largeCoverUrl = coverUrl ?: manga.largeCoverUrl,

			tags = tags.ifEmpty { manga.tags },

			chapters = chapters,

		)

	}



	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val (projectId, chapterId) = parseChapterRef(chapter.url)

		val doc = fetchProjectDocument(projectId, preferWebView = config[authSessionKey])

		if (doc != null && isChapterLoginRequired(doc, chapterId)) {
			throw AuthRequiredException(
				source,
				IllegalStateException(
					"Este capítulo exige login. Saia e entre de novo em Fontes → Blackout Comics → Entrar na conta.",
				),
			)
		}

		loadChapterPagesViaJs(projectId, chapterId)?.let { pages ->
			if (pages.isNotEmpty()) return pages
		}

		resolveReaderUrl(doc, projectId, chapterId)?.let { readerUrl ->
			loadReaderPagesViaJs(readerUrl)?.let { pages ->
				if (pages.isNotEmpty()) return pages
			}
			fetchPagesFromReaderUrl(readerUrl).takeIf { it.isNotEmpty() }?.let { return it }
		}

		fetchPagesViaWebView(projectId, chapterId)?.let { pages ->
			if (pages.isNotEmpty()) return pages
		}

		val storageKey = doc?.let { extractProjectStorageKey(it) }
		if (storageKey != null) {
			val chapterNum = doc.let { chapterNumberFromDoc(it, chapterId) } ?: chapter.number
			probeStorageChapterPages(storageKey, chapterNum).takeIf { it.isNotEmpty() }?.let { return it }
		}

		throw AuthRequiredException(
			source,
			IllegalStateException(
				"Não foi possível carregar as páginas. Entre em Fontes → Blackout Comics → Entrar na conta.",
			),
		)
	}

	private suspend fun tryLoadPagesFromReaderUrls(projectId: String, chapterId: String): List<MangaPage>? {
		val paths = listOf(
			"/comics/$projectId/chapter/$chapterId",
			"/comics/$projectId/read/$chapterId",
			"/comics/$projectId/capitulo/$chapterId",
			"/comics/$projectId/ler/$chapterId",
			"/comics/$projectId/chapter/$chapterId/read",
			"/comics/$projectId/$chapterId",
		)
		for (path in paths) {
			tryLoadPagesFromReaderUrl("https://$domain$path")?.let { return it }
		}
		return null
	}

	private suspend fun tryLoadPagesFromReaderUrl(readerUrl: String): List<MangaPage>? {
		loadReaderPagesViaJs(readerUrl)?.takeIf { it.isNotEmpty() }?.let { return it }
		fetchPagesFromReaderUrl(readerUrl).takeIf { it.isNotEmpty() }?.let { return it }
		return null
	}



	private suspend fun parseJsonResponse(url: String): org.json.JSONObject? {

		val response = webClient.httpGet(url, getJsonHeaders())

		return response.use {

			if (!it.isSuccessful) {

				return@use null

			}

			val raw = it.body.string().trim()

			if (raw.isEmpty() || raw.startsWith("<")) {

				return@use null

			}

			runCatching { org.json.JSONObject(raw) }.getOrNull()

		}

	}



	private suspend fun getOrCreateGenres(): Set<MangaTag> = genresMutex.withLock {
		cachedGenres?.let { return it }
		val doc = runCatching {
			webClient.httpGet("https://$domain/comics", getHtmlHeaders()).parseHtml()
		}.getOrNull()
		val tags = doc?.let(::parseGenresFromDocument).orEmpty()
		cachedGenres = tags
		tags
	}

	private fun parseGenresFromDocument(doc: Document): Set<MangaTag> {
		return doc.select(".genres-list a[href*='gen=']").mapNotNullToSet { anchor ->
			val href = anchor.attr("abs:href")
			val genre = href.substringAfter("gen=", "").substringBefore('&').urlDecode().nullIfEmpty()
				?: return@mapNotNullToSet null
			val title = anchor.text().trim().nullIfEmpty() ?: genre
			MangaTag(
				key = genre,
				title = title,
				source = source,
			)
		}
	}

	private fun buildComicsListUrl(page: Int, filter: MangaListFilter, json: Boolean = true): String {
		return "https://$domain/comics".toHttpUrl().newBuilder()
			.addQueryParameter("page", page.toString())
			.apply {
				if (json) {
					addQueryParameter("format", "json")
				}
				filter.tags.firstOrNull()?.key?.let { addQueryParameter("gen", it) }
				when (filter.states.singleOrNull()) {
					MangaState.FINISHED -> addQueryParameter("status", "completed")
					MangaState.ONGOING -> addQueryParameter("status", "ongoing")
					else -> Unit
				}
			}
			.build()
			.toString()
	}

	private suspend fun parseListFromHtml(page: Int, filter: MangaListFilter = MangaListFilter()): List<Manga> {

		val doc = webClient.httpGet(buildComicsListUrl(page, filter, json = false), getHtmlHeaders()).parseHtml()

		return doc.select("a[href^=/comics/]").mapNotNull { anchor ->

			val href = anchor.attr("href").trim()

			val id = href.removePrefix("/comics/").trimEnd('/').toIntOrNull() ?: return@mapNotNull null

			val title = anchor.attr("title").takeUnless { it.isBlank() }

				?: anchor.selectFirst("img")?.attr("alt")?.takeUnless { it.isBlank() }

				?: anchor.text().trim().takeIf { it.isNotBlank() }

				?: return@mapNotNull null

			val url = "/comics/$id"

			Manga(

				id = generateUid(url),

				title = title,

				altTitles = emptySet(),

				url = url,

				publicUrl = "https://$domain$url",

				rating = RATING_UNKNOWN,

				contentRating = null,

				coverUrl = anchor.selectFirst("img")?.attr("src")?.toAbsoluteUrl(domain),

				tags = emptySet(),

				state = null,

				authors = emptySet(),

				largeCoverUrl = null,

				description = null,

				source = source,

			)

		}.distinctBy { it.id }

	}



	private suspend fun searchComics(query: String, filter: MangaListFilter = MangaListFilter()): List<Manga> {

		val url = "https://$domain/comics".toHttpUrl().newBuilder()
			.addQueryParameter("src", query)
			.addQueryParameter("format", "json")
			.apply {
				filter.tags.firstOrNull()?.key?.let { addQueryParameter("gen", it) }
				when (filter.states.singleOrNull()) {
					MangaState.FINISHED -> addQueryParameter("status", "completed")
					MangaState.ONGOING -> addQueryParameter("status", "ongoing")
					else -> Unit
				}
			}
			.build()
			.toString()

		parseJsonResponse(url)?.optJSONArray("items")?.let { items ->

			if (items.length() > 0) {

				return items.mapJSON { parseMangaFromJson(it) }

			}

		}

		val htmlUrl = url.toHttpUrl().newBuilder()
			.removeAllQueryParameters("format")
			.build()
			.toString()

		val doc = webClient.httpGet(htmlUrl, getHtmlHeaders()).parseHtml()

		return doc.select("a[href^=/comics/]").mapNotNull { anchor ->

			val href = anchor.attr("href").trim()

			val id = href.removePrefix("/comics/").trimEnd('/').toIntOrNull() ?: return@mapNotNull null

			val title = anchor.text().trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null

			if (!title.contains(query, ignoreCase = true)) return@mapNotNull null

			parseMangaFromJson(

				org.json.JSONObject()

					.put("PJT_ID", id)

					.put("PJT_NAME", title)

					.put("PJT_IMG_PR_URL", anchor.selectFirst("img")?.attr("src").orEmpty()),

			)

		}.distinctBy { it.id }

	}



	private fun parseMangaFromJson(obj: org.json.JSONObject): Manga {

		val id = obj.getInt("PJT_ID")

		val url = "/comics/$id"

		return Manga(

			id = generateUid(url),

			title = obj.getString("PJT_NAME"),

			altTitles = emptySet(),

			url = url,

			publicUrl = "https://$domain$url",

			rating = RATING_UNKNOWN,

			contentRating = null,

			coverUrl = obj.optString("PJT_IMG_PR_URL").takeUnless { it.isBlank() }

				?: obj.optString("PJT_IMG_PR").takeUnless { it.isBlank() }?.let { "https://$domain/$it" },

			tags = emptySet(),

			state = null,

			authors = emptySet(),

			largeCoverUrl = null,

			description = null,

			source = source,

		)

	}



	private fun parseChapters(doc: Document, projectId: String): List<MangaChapter> {

		val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

		return doc.select("#tab-capitulos-list li.normal_ep, #tab-capitulos-list .chapter-item, li.normal_ep").mapNotNull { li ->

			val numberText = li.selectFirst(".num")?.text()?.trim() ?: return@mapNotNull null

			val number = numberText.toFloatOrNull() ?: return@mapNotNull null

			val chapterId = li.select("[data-chapter-rating-bar]").firstOrNull()

				?.attr("data-chapter-rating-bar")

				?.takeUnless { it.isBlank() }

				?: return@mapNotNull null

			val uploadDate = li.selectFirst("time[datetime]")?.attr("datetime")

				?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() }

				?: 0L

			val chapterUrl = "/comics/$projectId/chapter/$chapterId"

			MangaChapter(

				id = generateUid(chapterUrl),

				title = "Capítulo $numberText",

				number = number,

				volume = 0,

				url = chapterUrl,

				scanlator = null,

				uploadDate = uploadDate,

				branch = null,

				source = source,

			)

		}.distinctBy { it.url }.sortedBy { it.number }

	}



	private fun parseChapterRef(chapterUrl: String): Pair<String, String> {

		val match = CHAPTER_URL_REGEX.matchEntire(chapterUrl)

			?: throw ParseException("Formato de URL de capítulo inválido", chapterUrl)

		return match.groupValues[1] to match.groupValues[2]

	}



	private suspend fun fetchProjectStorageKey(projectId: String): String? {
		val doc = fetchProjectDocument(projectId, preferWebView = true) ?: return null
		return extractProjectStorageKey(doc)
	}

	private suspend fun fetchProjectDocument(projectId: String, preferWebView: Boolean = false): Document? {
		val url = "https://$domain/comics/$projectId"
		val useWebViewFirst = preferWebView || config[authSessionKey]
		if (useWebViewFirst) {
			loadProjectHtmlViaWebView(url)?.let { html ->
				val webDoc = Jsoup.parse(html, url)
				if (parseChapters(webDoc, projectId).isNotEmpty()) {
					return webDoc
				}
			}
		}
		val httpDoc = runCatching {
			webClient.httpGet(url, getHtmlHeaders()).parseHtml()
		}.getOrNull()
		if (httpDoc != null && parseChapters(httpDoc, projectId).isNotEmpty()) {
			return httpDoc
		}
		if (!useWebViewFirst) {
			loadProjectHtmlViaWebView(url)?.let { html ->
				val webDoc = Jsoup.parse(html, url)
				if (parseChapters(webDoc, projectId).isNotEmpty()) {
					return webDoc
				}
			}
		}
		return httpDoc
	}

	private suspend fun verifyLoginActive(): Boolean? {
		verifyLoginViaFetchJs()?.let { return it }
		val script = wrapWebViewScript("""
			(function() {
				if (document.querySelector('a[href*="sair"], a[href*="logout"], a[href*="perfil"], a[href*="minha-conta"], .user-menu, .nav-user, [data-user-name], .user-avatar, .header-user')) {
					return true;
				}
				var logins = document.querySelectorAll('#toggle-login, a.login, .header-right .login, a[href="/entrar"]');
				if (logins.length === 0) return true;
				for (var i = 0; i < logins.length; i++) {
					var login = logins[i];
					var style = window.getComputedStyle(login);
					if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') continue;
					var rect = login.getBoundingClientRect();
					if (rect.width >= 1 && rect.height >= 1) return false;
				}
				return true;
			})();
		""".trimIndent())
		val raw = evalJs("https://$domain/", script, timeout = 18_000L) ?: return null
		val domOk = parseJsBoolean(raw) ?: return null
		if (domOk) {
			return true
		}
		return verifyLoginViaFetchJs() ?: false
	}

	private suspend fun verifyLoginViaFetchJs(): Boolean? {
		val probeUrl = authVerifyUrl
		val script = wrapWebViewPromise("auth-probe", """
			fetch('$probeUrl', { credentials: 'include', redirect: 'follow' })
				.then(function(response) { return response.text(); })
				.then(function(html) {
					var locked = 0;
					var readable = 0;
					var re = /chapter-link-wrap[^>]*onclick="([^"]*)"/g;
					var match;
					while ((match = re.exec(html)) !== null) {
						if (match[1].indexOf('showLoginModal') >= 0) {
							locked++;
						} else if (match[1].indexOf('location.href') >= 0) {
							readable++;
						}
					}
					if (locked > 0) finish(false);
					else if (readable > 0) finish(true);
					else finish(null);
				})
				.catch(function() { finish(null); });
		""".trimIndent())
		val raw = evalJs("https://$domain/", script, timeout = 20_000L) ?: return null
		return when (raw.trim().trim('"')) {
			"true" -> true
			"false" -> false
			else -> null
		}
	}

	private fun webViewBootstrapScript(): String = """
		(function() {
			try {
				var now = Date.now();
				var ttl = 7 * 24 * 60 * 60 * 1000;
				var data = { consentAt: now, expiresAt: now + ttl };
				localStorage.setItem('age_gate_consent', JSON.stringify(data));
				document.documentElement.classList.add('age-gate-accepted');
				var gate = document.getElementById('ageGate');
				if (gate) gate.style.display = 'none';
			} catch (e) {}
		})();
	""".trimIndent()

	private fun wrapWebViewScript(script: String): String = buildString {
		append("window.__evaluateJsDone = undefined;\n")
		append(webViewBootstrapScript())
		append('\n')
		append(script)
	}

	private fun wrapWebViewPromise(key: String, promiseBody: String): String = wrapWebViewScript("""
		(function() {
			var cacheKey = '__kosenPromise_$key';
			if (window[cacheKey]) return window[cacheKey];
			window[cacheKey] = new Promise(function(resolve) {
				function finish(value) {
					window.__evaluateJsDone = value;
					resolve(value);
				}
				$promiseBody
			});
			return window[cacheKey];
		})();
	""".trimIndent())

	private suspend fun loadProjectHtmlViaWebView(url: String): String? {
		val script = wrapWebViewPromise("project-html", """
			var start = Date.now();
			function tick() {
				var chapters = document.querySelectorAll('#tab-capitulos-list li.normal_ep, #tab-capitulos-list .chapter-item, li.normal_ep');
				if (chapters.length > 0) {
					finish(document.documentElement ? document.documentElement.outerHTML : '');
					return;
				}
				if (Date.now() - start > 12000) {
					finish(null);
				} else {
					setTimeout(tick, 350);
				}
			}
			tick();
		""".trimIndent())
		val raw = evalJs(url, script, timeout = 16_000L) ?: return null
		val decoded = decodeEvaluateJsHtml(raw).trim()
		if (decoded.isEmpty() || decoded == "null") return null
		return decoded
	}

	private suspend fun loadChapterPagesViaJs(projectId: String, chapterId: String): List<MangaPage>? {
		val pageUrl = "https://$domain/comics/$projectId"
		val script = wrapWebViewPromise(
			"chapter-pages-$projectId-$chapterId",
			buildChapterReaderPromiseBody(projectId, chapterId, navigate = true),
		)
		return parsePagesFromJsResult(evalJs(pageUrl, script, timeout = 24_000L))
	}

	private suspend fun loadReaderPagesViaJs(readerUrl: String): List<MangaPage>? {
		val script = wrapWebViewPromise(
			"reader-pages-$readerUrl",
			buildChapterReaderPromiseBody(projectId = "", chapterId = "", navigate = false),
		)
		return parsePagesFromJsResult(evalJs(readerUrl, script, timeout = 20_000L))
	}

	private fun buildChapterReaderPromiseBody(projectId: String, chapterId: String, navigate: Boolean): String {
		val navigateBlock = if (navigate) """
			function tryChapterClick() {
				var bar = document.querySelector('[data-chapter-rating-bar="$chapterId"]');
				if (!bar) { finish('[]'); return; }
				var li = bar.closest('li');
				var wrap = li ? li.querySelector('.chapter-link-wrap') : null;
				var oc = wrap ? (wrap.getAttribute('onclick') || '') : '';
				if (oc.indexOf('showLoginModal') >= 0) { finish('LOGIN_REQUIRED'); return; }
				var hm = oc.match(/location\\.href\\s*=\\s*['"]([^'"]+)['"]/);
				if (hm) {
					var path = hm[1];
					window.location.href = path.indexOf('http') === 0 ? path
						: (window.location.origin + (path.indexOf('/') === 0 ? path : '/' + path));
					afterNav();
					return;
				}
				var a = li ? li.querySelector('a[href]') : null;
				if (a && a.href && a.href.indexOf('javascript') < 0) {
					window.location.href = a.href;
					afterNav();
					return;
				}
				if (wrap) wrap.click();
				afterNav();
			}
			tryChapterClick();
		""" else """
			afterNav();
		"""
		return """
			function collect() {
				var urls = [];
				var re = /https:\\/\\/(?:blackoutcomics\\.com|cdn\\.blackoutcomics\\.com)\\/projects\\/[^"'\\s]+?\\/chapters\\/[^"'\\s]+?\\/[^"'\\s?#]+?\\.(?:webp|jpg|jpeg|png)/gi;
				var html = document.documentElement ? document.documentElement.outerHTML : '';
				var m;
				while ((m = re.exec(html)) !== null) { urls.push(m[0]); }
				document.querySelectorAll('img').forEach(function(img) {
					var s = img.src || img.getAttribute('data-src') || '';
					if (s.indexOf('/projects/') >= 0 && s.indexOf('/chapters/') >= 0 && s.indexOf('/previews/') < 0) {
						urls.push(s);
					}
				});
				var seen = {};
				var unique = [];
				urls.forEach(function(u) {
					if (!seen[u]) { seen[u] = 1; unique.push(u); }
				});
				finish(JSON.stringify(unique));
			}
			function afterNav() {
				var start = Date.now();
				function tick() {
					var n = document.querySelectorAll(
						'img[src*="/projects/"][src*="/chapters/"], .reader img, .reading-content img'
					).length;
					if (n >= 1 || Date.now() - start > 14000) {
						collect();
					} else {
						setTimeout(tick, 350);
					}
				}
				setTimeout(tick, 500);
			}
			$navigateBlock
		""".trimIndent()
	}

	private fun parsePagesFromJsResult(raw: String?): List<MangaPage>? {
		val decoded = raw?.let { decodeEvaluateJsHtml(it).trim() } ?: return null
		if (decoded == "LOGIN_REQUIRED") {
			throw AuthRequiredException(
				source,
				IllegalStateException("Login necessário para ler este capítulo."),
			)
		}
		if (decoded.isEmpty() || decoded == "[]") return null
		val urls = runCatching {
			val arr = org.json.JSONArray(decoded)
			(0 until arr.length()).mapNotNull { i ->
				arr.optString(i).takeIf { u ->
					u.startsWith("http") && u.contains("/projects/") && !u.contains("/previews/")
				}
			}
		}.getOrElse { parsePageUrls(decoded) }
		if (urls.isEmpty()) return null
		return sortPageUrls(urls).map { url ->
			MangaPage(
				id = generateUid(url),
				url = url,
				preview = null,
				source = source,
			)
		}
	}

	private suspend fun fetchPagesViaWebView(projectId: String, chapterId: String): List<MangaPage>? {
		val pageUrl = "https://$domain/comics/$projectId"
		val pageScript = wrapWebViewScript("""
			(function() {
				const bar = document.querySelector('[data-chapter-rating-bar="$chapterId"]');
				if (!bar) return;
				const li = bar.closest('li');
				const wrap = li && li.querySelector('.chapter-link-wrap');
				if (!wrap) return;
				const oc = wrap.getAttribute('onclick') || '';
				if (oc.indexOf('showLoginModal') >= 0) return;
				const m = oc.match(/location\\.href\\s*=\\s*['"]([^'"]+)['"]/);
				if (m) {
					const path = m[1];
					window.location.href = path.startsWith('http') ? path
						: (window.location.origin + (path.startsWith('/') ? path : '/' + path));
					return;
				}
				const a = li.querySelector('a[href]');
				if (a && a.href && a.href.indexOf('javascript') < 0) {
					window.location.href = a.href;
					return;
				}
				wrap.click();
			})();
		""".trimIndent())
		val config = InterceptionConfig(
			timeoutMs = 22_000L,
			urlPattern = PAGE_URL_PATTERN,
			pageScript = pageScript,
			maxRequests = 120,
			userAgent = parserUserAgent,
		)
		return runCatching {
			context.interceptWebViewRequests(pageUrl, config)
				.map { it.url }
				.distinct()
				.let { sortPageUrls(it) }
				.map { url ->
					MangaPage(
						id = generateUid(url),
						url = url,
						preview = null,
						source = source,
					)
				}
		}.getOrNull()
	}

	private fun sortPageUrls(urls: List<String>): List<String> {
		return urls.sortedBy { url ->
			url.substringAfterLast('/').substringBeforeLast('.').filter { it.isDigit() }.toIntOrNull() ?: 0
		}
	}



	private fun extractProjectStorageKey(doc: Document): String? {

		val imageUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")

			?: doc.selectFirst(".project-cover img")?.attr("src")

			?: return null

		return STORAGE_KEY_REGEX.find(imageUrl)?.groupValues?.get(1)

	}



	private fun hasReadableChapter(doc: Document): Boolean {
		val wraps = doc.select(".chapter-link-wrap")
		if (wraps.isEmpty()) return false
		return wraps.any { !it.attr("onclick").contains("showLoginModal", ignoreCase = true) }
	}

	private fun chapterNumberFromDoc(doc: Document, chapterId: String): Float? {
		val bar = doc.selectFirst("[data-chapter-rating-bar=$chapterId]") ?: return null
		val li = bar.closest("li") ?: return null
		val numText = li.selectFirst(".num")?.text()?.trim() ?: return null
		return numText.toFloatOrNull()
	}

	private fun isChapterLoginRequired(doc: Document, chapterId: String): Boolean {
		val bar = doc.selectFirst("[data-chapter-rating-bar=$chapterId]") ?: return false
		val onclick = bar.closest("li")?.selectFirst(".chapter-link-wrap")?.attr("onclick").orEmpty()
		return onclick.contains("showLoginModal")
	}

	private fun isLoginRequiredOnChapterList(doc: Document): Boolean = !hasReadableChapter(doc)



	private suspend fun resolveReaderUrl(
		doc: Document?,
		projectId: String,
		chapterId: String,
	): String? {
		val document = doc ?: fetchProjectDocument(projectId, preferWebView = true) ?: return null
		val bar = document.selectFirst("[data-chapter-rating-bar=$chapterId]") ?: return null

		val li = bar.closest("li") ?: return null

		li.selectFirst("a[href]")?.attr("abs:href")?.takeUnless { href ->

			href.isBlank() || href.startsWith("javascript", ignoreCase = true)

		}?.let { return it }

		val onclick = li.selectFirst(".chapter-link-wrap")?.attr("onclick").orEmpty()

		if (onclick.contains("showLoginModal")) {

			return null

		}

		READER_URL_REGEX.find(onclick)?.groupValues?.get(1)?.let { path ->

			return if (path.startsWith("http")) {

				path

			} else {

				"https://$domain${if (path.startsWith("/")) path else "/$path"}"

			}

		}

		return null

	}



	private suspend fun fetchPagesFromReaderUrl(readerUrl: String): List<MangaPage> {
		val html = runCatching {
			webClient.httpGet(readerUrl, getHtmlHeaders()).parseHtml().html()
		}.getOrElse {
			loadProjectHtmlViaWebView(readerUrl) ?: return emptyList()
		}
		return parsePageUrls(html).map { url ->

			MangaPage(

				id = generateUid(url),

				url = url,

				preview = null,

				source = source,

			)

		}

	}



	private suspend fun loadReaderHtmlWithJs(projectId: String, chapterId: String): String? {

		val pageUrl = "https://$domain/comics/$projectId"

		val script = wrapWebViewScript("""

			new Promise((resolve) => {

				const bar = document.querySelector('[data-chapter-rating-bar="$chapterId"]');

				if (!bar) { resolve(""); return; }

				const li = bar.closest('li');

				const wrap = li && li.querySelector('.chapter-link-wrap');

				const oc = wrap ? (wrap.getAttribute('onclick') || '') : '';

				if (oc.indexOf('showLoginModal') >= 0) {

					resolve("");

					return;

				}

				const m = oc.match(/location\\.href\\s*=\\s*['"]([^'"]+)['"]/);

				if (m) {

					const path = m[1];

					window.location.assign(path.startsWith('http') ? path

						: (window.location.origin + (path.startsWith('/') ? path : '/' + path)));

				} else {

					const link = li && li.querySelector('a[href]');

					if (!link || !link.href || link.href.indexOf('javascript') >= 0) {

						resolve(document.documentElement ? document.documentElement.outerHTML : "");

						return;

					}

					window.location.assign(link.href);

				}

				const start = Date.now();

				const tick = () => {

					const hasReader = document.querySelector('.btn-prev-chapter, .btn-next-chapter, .reader, #reader');

					const imgs = document.querySelectorAll(

						'.reader img[src], .reading-content img[src], #pages img[src], img.list_lazy[src]'

					);

					if (hasReader || imgs.length > 2 || Date.now() - start > 24000) {

						resolve(document.documentElement ? document.documentElement.outerHTML : "");

					} else {

						setTimeout(tick, 400);

					}

				};

				setTimeout(tick, 700);

			});

		""".trimIndent())

		val raw = evalJs(pageUrl, script, timeout = 15_000L) ?: return null

		return decodeEvaluateJsHtml(raw).takeUnless { it.isBlank() }

	}



	private fun parseJsBoolean(raw: String): Boolean? = when (decodeEvaluateJsHtml(raw).trim().lowercase()) {
		"true" -> true
		"false" -> false
		else -> null
	}

	private fun decodeEvaluateJsHtml(raw: String): String {

		val value = raw.trim()

		if (value.length >= 2 && value.first() == '"' && value.last() == '"') {

			val unescaped = value.substring(1, value.length - 1)

				.replace("\\\\", "\\")

				.replace("\\\"", "\"")

				.replace("\\n", "\n")

				.replace("\\r", "\r")

				.replace("\\t", "\t")

			return unescaped.replace(Regex("""\\u([0-9a-fA-F]{4})""")) { m ->

				m.groupValues[1].toInt(16).toChar().toString()

			}

		}

		return value

	}



	private fun parsePageUrls(html: String): List<String> {

		val doc = Jsoup.parse(html)

		val urls = LinkedHashSet<String>()

		doc.select(

			".reader img, .reader-pages img, #reader img, .reading-content img, " +

				".chapter-content img, .page-chapter img, img.list_lazy, img[data-src]",

		).forEach { img ->

			val src = img.absUrl("src").ifBlank { img.attr("data-src") }

			if (src.startsWith("http") && src.contains("/projects/") && !src.contains("/previews/")) {

				urls.add(src)

			}

		}

		PAGE_IMAGE_REGEX.findAll(html).forEach { match ->

			val url = match.value

			if (!url.contains("/previews/")) {

				urls.add(url)

			}

		}

		return urls.toList()

	}



	private suspend fun probeStorageChapterPages(storageKey: String, chapterNumber: Float): List<MangaPage> {

		val folder = chapterFolderName(chapterNumber)

		val base = "https://$domain/projects/$storageKey/chapters/$folder/"

		val result = ArrayList<MangaPage>()

		var consecutiveMiss = 0

		var pageIndex = 1

		while (pageIndex <= 80 && consecutiveMiss < 2) {

			val pageName = pageIndex.toString().padStart(3, '0')

			var found = false

			for (ext in arrayOf("webp", "jpg")) {

				val url = "${base}${pageName}.$ext"

				val exists = runCatching {

					webClient.httpHead(url.toHttpUrl()).use { it.isSuccessful }

				}.getOrDefault(false)

				if (exists) {

					result.add(

						MangaPage(

							id = generateUid(url),

							url = url,

							preview = null,

							source = source,

						),

					)

					found = true

					consecutiveMiss = 0

					break

				}

			}

			if (!found) {

				consecutiveMiss++

			}

			pageIndex++

		}

		return result

	}



	private fun chapterFolderName(number: Float): String {

		val intValue = number.toInt()

		return if (number == intValue.toFloat()) {

			intValue.toString().padStart(2, '0')

		} else {

			number.toString()

		}

	}



	private companion object {

		/** Obra pública com capítulos bloqueados sem login; usada só para `isAuthorized()`. */
		const val AUTH_PROBE_COMIC_ID = "177"

		val PAGE_URL_PATTERN = Regex(
			"""https://(?:blackoutcomics\.com|cdn\.blackoutcomics\.com)/projects/[^"'\\s]+/chapters/[^"'\\s]+/[^"'\\s?#]+\.(?:webp|jpg|jpeg|png)""",
			RegexOption.IGNORE_CASE,
		)

		val CHAPTER_URL_REGEX = Regex("""^/comics/([^/]+)/chapter/([^/]+)$""")

		val STORAGE_KEY_REGEX = Regex("""/projects/([^/]+)/""")

		val READER_URL_REGEX = Regex("""location\.href\s*=\s*['"]([^'"]+)['"]""")

		val PAGE_IMAGE_REGEX = Regex(

			"""https://(?:blackoutcomics\.com|cdn\.blackoutcomics\.com)/projects/[^"'\s<>]+?\.(?:webp|jpg|jpeg|png)""",

		)

		val PAGE_EXTENSIONS = arrayOf("webp", "jpg", "jpeg", "png")

	}

}


