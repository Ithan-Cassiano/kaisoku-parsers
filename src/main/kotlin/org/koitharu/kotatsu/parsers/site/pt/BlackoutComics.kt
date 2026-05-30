package org.koitharu.kotatsu.parsers.site.pt



import okhttp3.Headers

import okhttp3.HttpUrl.Companion.toHttpUrl

import okhttp3.Interceptor

import okhttp3.Response

import org.jsoup.Jsoup

import org.jsoup.nodes.Document

import org.koitharu.kotatsu.parsers.MangaLoaderContext

import org.koitharu.kotatsu.parsers.MangaParserAuthProvider

import org.koitharu.kotatsu.parsers.MangaSourceParser

import org.koitharu.kotatsu.parsers.config.ConfigKey

import org.koitharu.kotatsu.parsers.core.PagedMangaParser

import org.koitharu.kotatsu.parsers.exception.AuthRequiredException

import org.koitharu.kotatsu.parsers.exception.ParseException

import org.koitharu.kotatsu.parsers.model.*

import org.koitharu.kotatsu.parsers.network.UserAgents

import org.koitharu.kotatsu.parsers.util.*

import org.koitharu.kotatsu.parsers.util.json.mapJSON

import org.koitharu.kotatsu.parsers.webview.InterceptionConfig

import java.text.SimpleDateFormat

import java.util.*



@MangaSourceParser("BLACKOUT_COMICS", "Blackout Comics", "pt", ContentType.COMICS)

internal class BlackoutComics(context: MangaLoaderContext) :

	PagedMangaParser(context, MangaParserSource.BLACKOUT_COMICS, pageSize = 12),

	MangaParserAuthProvider {



	override val configKeyDomain = ConfigKey.Domain("blackoutcomics.com")

	override val userAgentKey = ConfigKey.UserAgent(UserAgents.CHROME_MOBILE)

	private val authSessionKey = ConfigKey.AuthSession(defaultValue = false)

	override val sourceLocale: Locale = Locale("pt", "BR")

	override val authUrl: String
		get() = "https://$domain/entrar"

	override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
		super.onCreateConfig(keys)
		keys.add(userAgentKey)
		keys.add(authSessionKey)
		keys.add(ConfigKey.InterceptCloudflare(defaultValue = true))
		keys.add(ConfigKey.DisableUpdateChecking(defaultValue = true))
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
		if (config[authSessionKey]) {
			return true
		}
		if (verifyLoginActive() == true) {
			config[authSessionKey] = true
			return true
		}
		return false
	}

	override suspend fun getUsername(): String {
		if (!config[authSessionKey]) {
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



	override suspend fun getFilterOptions() = MangaListFilterOptions()



	override val availableSortOrders: Set<SortOrder> = EnumSet.of(

		SortOrder.ALPHABETICAL,

		SortOrder.UPDATED,

	)



	override val filterCapabilities: MangaListFilterCapabilities

		get() = MangaListFilterCapabilities(

			isSearchSupported = true,

		)



	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {

		if (!filter.query.isNullOrEmpty()) {

			return searchComics(filter.query)

		}

		val url = buildString {

			append("https://")

			append(domain)

			append("/comics?format=json&page=")

			append(page.toString())

		}

		parseJsonResponse(url)?.optJSONArray("items")?.let { items ->
			return items.mapJSON { parseMangaFromJson(it) }
		}

		return parseListFromHtml(page)

	}



	override suspend fun getDetails(manga: Manga): Manga {

		val doc = webClient.httpGet(manga.url.toAbsoluteUrl(domain), getHtmlHeaders()).parseHtml()

		val projectId = manga.url.removePrefix("/comics/").trimEnd('/')

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

		val chapters = parseChapters(doc, projectId)

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
		if (!config[authSessionKey]) {
			throw AuthRequiredException(
				source,
				IllegalStateException("Faça login em Configurações → Fontes → Blackout Comics → Entrar na conta e use \"Confirmar login\"."),
			)
		}

		tryLoadPagesFromReaderUrl(chapter.url.toAbsoluteUrl(domain))?.let { return it }
		tryLoadPagesFromReaderUrls(projectId, chapterId)?.let { return it }

		fetchPagesViaWebView(projectId, chapterId)?.let { pages ->
			if (pages.isNotEmpty()) return pages
		}

		loadChapterPagesViaJs(projectId, chapterId)?.let { pages ->
			if (pages.isNotEmpty()) return pages
		}

		val doc = fetchProjectDocument(projectId, preferWebView = true)
		resolveReaderUrl(doc, projectId, chapterId)?.let { readerUrl ->
			tryLoadPagesFromReaderUrl(readerUrl)?.let { return it }
		}

		val storageKey = doc?.let { extractProjectStorageKey(it) }
			?: fetchProjectStorageKey(projectId)
		if (storageKey != null) {
			val chapterNum = doc?.let { chapterNumberFromDoc(it, chapterId) } ?: chapter.number
			val pages = probeStorageChapterPages(storageKey, chapterNum)
			if (pages.isNotEmpty()) return pages
		}

		throw ParseException(
			"Não foi possível carregar as páginas. Volte em Fontes → Blackout Comics, entre de novo e use \"Confirmar login\" no menu (⋮).",
			chapter.url,
		)
	}

	private suspend fun tryLoadPagesFromReaderUrls(projectId: String, chapterId: String): List<MangaPage>? {
		val paths = listOf(
			"/comics/$projectId/chapter/$chapterId",
			"/comics/$projectId/read/$chapterId",
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



	private suspend fun parseListFromHtml(page: Int): List<Manga> {

		val doc = webClient.httpGet("https://$domain/comics?page=$page", getHtmlHeaders()).parseHtml()

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



	private suspend fun searchComics(query: String): List<Manga> {

		val url = "https://$domain/comics?src=${query.urlEncoded()}&format=json"

		parseJsonResponse(url)?.optJSONArray("items")?.let { items ->

			if (items.length() > 0) {

				return items.mapJSON { parseMangaFromJson(it) }

			}

		}

		val doc = webClient.httpGet(

			"https://$domain/comics?src=${query.urlEncoded()}",

			getHtmlHeaders(),

		).parseHtml()

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

		return doc.select("#tab-capitulos-list li.normal_ep, #tab-capitulos-list .chapter-item").mapNotNull { li ->

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

				title = li.selectFirst(".chapters-modal-num")?.text()

					?: "Capítulo $numberText",

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
			loadProjectHtmlViaWebView(url)?.let { return Jsoup.parse(it, url) }
		}
		val httpDoc = runCatching {
			webClient.httpGet(url, getHtmlHeaders()).parseHtml()
		}.getOrNull()
		if (httpDoc != null && httpDoc.select(".chapter-link-wrap").isNotEmpty()) {
			return httpDoc
		}
		if (!useWebViewFirst) {
			loadProjectHtmlViaWebView(url)?.let { return Jsoup.parse(it, url) }
		}
		return httpDoc
	}

	private suspend fun verifyLoginActive(): Boolean? {
		val script = """
			(function() {
				var login = document.querySelector('#toggle-login');
				if (!login) return true;
				var style = window.getComputedStyle(login);
				if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return true;
				var rect = login.getBoundingClientRect();
				if (rect.width < 1 || rect.height < 1) return true;
				return false;
			})();
		""".trimIndent()
		val raw = context.evaluateJs("https://$domain/", script, timeout = 18_000L) ?: return null
		return raw.contains("true")
	}

	private suspend fun loadProjectHtmlViaWebView(url: String): String? {
		val script = """
			(function() {
				return new Promise(function(resolve) {
					var start = Date.now();
					function tick() {
						var wraps = document.querySelectorAll('.chapter-link-wrap');
						if (wraps.length > 0) {
							resolve(document.documentElement ? document.documentElement.outerHTML : '');
							return;
						}
						if (Date.now() - start > 18000) {
							resolve(null);
						} else {
							setTimeout(tick, 350);
						}
					}
					tick();
				});
			})();
		""".trimIndent()
		val raw = context.evaluateJs(url, script, timeout = 22_000L) ?: return null
		val decoded = decodeEvaluateJsHtml(raw).trim()
		if (decoded.isEmpty() || decoded == "null") return null
		return decoded
	}

	private suspend fun loadChapterPagesViaJs(projectId: String, chapterId: String): List<MangaPage>? {
		val pageUrl = "https://$domain/comics/$projectId"
		val script = buildChapterReaderScript(projectId, chapterId, navigate = true)
		return parsePagesFromJsResult(context.evaluateJs(pageUrl, script, timeout = 35_000L))
	}

	private suspend fun loadReaderPagesViaJs(readerUrl: String): List<MangaPage>? {
		val script = buildChapterReaderScript(projectId = "", chapterId = "", navigate = false)
		return parsePagesFromJsResult(context.evaluateJs(readerUrl, script, timeout = 35_000L))
	}

	private fun buildChapterReaderScript(projectId: String, chapterId: String, navigate: Boolean): String {
		val navigateBlock = if (navigate) """
			var direct = [
				'/comics/$projectId/chapter/$chapterId',
				'/comics/$projectId/read/$chapterId'
			];
			var di = 0;
			function tryDirect() {
				if (di >= direct.length) { tryChapterClick(); return; }
				window.location.href = window.location.origin + direct[di++];
				setTimeout(function() {
					if (document.querySelectorAll('img[src*="/projects/"][src*="/chapters/"]').length >= 1) {
						collect();
					} else if (di < direct.length) {
						tryDirect();
					} else {
						tryChapterClick();
					}
				}, 2200);
			}
			function tryChapterClick() {
				var bar = document.querySelector('[data-chapter-rating-bar="$chapterId"]');
				if (!bar) { resolve('[]'); return; }
				var li = bar.closest('li');
				var wrap = li ? li.querySelector('.chapter-link-wrap') : null;
				var oc = wrap ? (wrap.getAttribute('onclick') || '') : '';
				if (oc.indexOf('showLoginModal') >= 0) { resolve('LOGIN_REQUIRED'); return; }
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
			tryDirect();
		""" else """
			afterNav();
		"""
		return """
			(function() {
				return new Promise(function(resolve) {
					function collect() {
						var urls = [];
						var re = /https:\/\/(?:blackoutcomics\.com|cdn\.blackoutcomics\.com)\/projects\/[^"'\\s]+?\/chapters\/[^"'\\s]+?\/[^"'\\s?#]+?\\.(?:webp|jpg|jpeg|png)/gi;
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
						resolve(JSON.stringify(unique));
					}
					function afterNav() {
						var start = Date.now();
						function tick() {
							var n = document.querySelectorAll(
								'img[src*="/projects/"][src*="/chapters/"], .reader img, .reading-content img'
							).length;
							if (n >= 1 || Date.now() - start > 22000) {
								collect();
							} else {
								setTimeout(tick, 350);
							}
						}
						setTimeout(tick, 500);
					}
					$navigateBlock
				});
			})();
		""".trimIndent()
	}

	private fun parsePagesFromJsResult(raw: String?): List<MangaPage>? {
		val decoded = raw?.let { decodeEvaluateJsHtml(it).trim() } ?: return null
		if (decoded == "LOGIN_REQUIRED") return null
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
		val pageScript = """
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
		""".trimIndent()
		val config = InterceptionConfig(
			timeoutMs = 30_000L,
			urlPattern = PAGE_URL_PATTERN,
			pageScript = pageScript,
			maxRequests = 120,
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
		val li = bar.parent() ?: return null
		val numText = li.selectFirst(".num")?.text()?.trim() ?: return null
		return numText.toFloatOrNull()
	}

	private fun isLoginRequiredOnChapterList(doc: Document): Boolean = !hasReadableChapter(doc)



	private suspend fun resolveReaderUrl(
		doc: Document?,
		projectId: String,
		chapterId: String,
	): String? {
		val document = doc ?: fetchProjectDocument(projectId, preferWebView = true) ?: return null
		if (!config[authSessionKey] && isLoginRequiredOnChapterList(document)) {
			return null
		}
		val bar = document.selectFirst("[data-chapter-rating-bar=$chapterId]") ?: return null

		val li = bar.parent() ?: return null

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

		val script = """

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

		""".trimIndent()

		val raw = context.evaluateJs(pageUrl, script, timeout = 15_000L) ?: return null

		return decodeEvaluateJsHtml(raw).takeUnless { it.isBlank() }

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

		while (pageIndex <= 120 && consecutiveMiss < 3) {

			val pageName = pageIndex.toString().padStart(3, '0')

			var found = false

			for (ext in PAGE_EXTENSIONS) {

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


