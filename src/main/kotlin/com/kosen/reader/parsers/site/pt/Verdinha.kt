package com.kosen.reader.parsers.site.pt

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.core.PagedMangaParser
import com.kosen.reader.parsers.exception.AuthRequiredException
import com.kosen.reader.parsers.exception.ParseException
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
import com.kosen.reader.parsers.network.UserAgents
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.getCookies
import com.kosen.reader.parsers.util.json.mapJSON
import com.kosen.reader.parsers.util.json.mapJSONNotNull
import com.kosen.reader.parsers.util.parseJson
import com.kosen.reader.parsers.util.parseSafe
import com.kosen.reader.parsers.util.toTitleCase
import java.text.SimpleDateFormat
import java.util.EnumSet
import java.util.Locale
import java.util.TimeZone

@MangaSourceParser("VERDINHA", "Verdinha", "pt")
internal class Verdinha(context: MangaLoaderContext) : PagedMangaParser(
	context,
	source = MangaParserSource.VERDINHA,
	pageSize = 24,
	searchPageSize = 15,
) {
	override val configKeyDomain = ConfigKey.Domain("verdinha.wtf")

	private val apiUrl = "https://api.verdinha.wtf"
	private val cdnUrl = "https://cdn.verdinha.wtf"
	private val defaultScanId = 1

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
		SortOrder.POPULARITY,
		SortOrder.POPULARITY_TODAY,
		SortOrder.POPULARITY_WEEK,
		SortOrder.POPULARITY_MONTH,
	)

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isSearchWithFiltersSupported = true,
			isMultipleTagsSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions {
		return MangaListFilterOptions(
			availableTags = fetchAvailableTags(),
			availableStates = EnumSet.of(
				MangaState.ONGOING,
				MangaState.FINISHED,
				MangaState.PAUSED,
				MangaState.ABANDONED,
			),
			availableContentTypes = EnumSet.of(
				ContentType.MANGA,
				ContentType.MANHUA,
				ContentType.MANHWA,
				ContentType.HENTAI,
			),
		)
	}

	private fun apiHeaders(scanId: Int = defaultScanId): Headers {
		val builder = Headers.Builder()
			.add("User-Agent", UserAgents.CHROME_MOBILE)
			.add("Accept", "application/json, text/plain, */*")
			.add("Origin", "https://$domain")
			.add("Referer", "https://$domain/")
			.add("scan-id", scanId.toString())
		resolveAccessToken()?.let { token ->
			builder.add("Authorization", "Bearer $token")
		}
		return builder.build()
	}

	private fun resolveAccessToken(): String? {
		val cookies = context.cookieJar.getCookies(domain) +
			context.cookieJar.getCookies("api.$domain")
		return cookies.firstOrNull { cookie ->
			cookie.name.equals("access_token", ignoreCase = true) && cookie.value.isNotBlank()
		}?.value
	}

	override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		val url = when {
			!filter.query.isNullOrEmpty() || filter.tags.isNotEmpty() ||
				filter.states.isNotEmpty() || filter.types.isNotEmpty() -> buildSearchUrl(page, filter, order)

			order in setOf(
				SortOrder.POPULARITY,
				SortOrder.POPULARITY_TODAY,
				SortOrder.POPULARITY_WEEK,
				SortOrder.POPULARITY_MONTH,
			) -> {
				val period = when (order) {
					SortOrder.POPULARITY_TODAY -> "dia"
					SortOrder.POPULARITY_WEEK -> "semana"
					SortOrder.POPULARITY_MONTH -> "mes"
					else -> "geral"
				}
				"$apiUrl/obras/ranking".toHttpUrl().newBuilder()
					.addQueryParameter("periodo", period)
					.addQueryParameter("limite", pageSize.toString())
					.addQueryParameter("pagina", page.toString())
					.build()
			}

			else -> buildSearchUrl(page, filter, order)
		}
		val response = webClient.httpGet(url, apiHeaders()).parseJson()
		val results = response.optJSONArray("obras") ?: return emptyList()
		return results.mapJSON { parseMangaFromJson(it) }
	}

	private fun buildSearchUrl(page: Int, filter: MangaListFilter, order: SortOrder): HttpUrl {
		val builder = "$apiUrl/obras/search".toHttpUrl().newBuilder()
			.addQueryParameter("pagina", page.toString())
			.addQueryParameter("limite", pageSize.toString())
			.addQueryParameter("todos_generos", "1")

		if (!filter.query.isNullOrEmpty()) {
			builder.addQueryParameter("obr_nome", filter.query)
		}

		when (order) {
			SortOrder.UPDATED -> {
				builder.addQueryParameter("orderBy", "ultima_atualizacao")
				builder.addQueryParameter("orderDirection", "DESC")
			}
			SortOrder.POPULARITY -> {
				builder.addQueryParameter("orderBy", "media_rating")
				builder.addQueryParameter("orderDirection", "DESC")
			}
			else -> {
				builder.addQueryParameter("orderBy", "ultima_atualizacao")
				builder.addQueryParameter("orderDirection", "DESC")
			}
		}

		filter.tags.forEach { tag ->
			builder.addQueryParameter("tags[]", tag.key)
		}

		filter.types.firstOrNull()?.let { contentType ->
			val type = when (contentType) {
				ContentType.MANHWA -> "1"
				ContentType.MANHUA -> "2"
				ContentType.MANGA -> "3"
				ContentType.HENTAI -> "5"
				else -> null
			}
			type?.let { builder.addQueryParameter("formt_id", it) }
		}

		filter.states.firstOrNull()?.let { state ->
			val statusId = when (state) {
				MangaState.ONGOING -> "1"
				MangaState.FINISHED -> "2"
				MangaState.PAUSED -> "3"
				MangaState.ABANDONED -> "4"
				else -> null
			}
			statusId?.let { builder.addQueryParameter("stt_id", it) }
		}

		return builder.build()
	}

	private fun parseMangaFromJson(json: JSONObject): Manga {
		val id = json.getInt("obr_id")
		val name = json.getString("obr_nome")
		val slug = json.optString("obr_slug", "").ifEmpty {
			name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
		}
		val scanId = json.optInt("scan_id", defaultScanId).takeIf { it > 0 } ?: defaultScanId
		val coverPath = json.optString("obr_imagem", "").takeIf { it != "null" && it.isNotEmpty() } ?: ""

		val coverUrl = when {
			coverPath.isEmpty() -> null
			coverPath.startsWith("http") -> coverPath
			coverPath.startsWith("wp-content") -> "$cdnUrl/$coverPath"
			else -> "$cdnUrl/scans/$scanId/obras/$id/$coverPath"
		}

		val genero = json.optJSONObject("genero")
		val genreName = genero?.optString("gen_nome", "") ?: ""
		val isNsfw = genreName.equals("hentai", ignoreCase = true)

		val rating = json.optDouble("media_rating", 0.0).let {
			if (it > 0) (it / 5.0).toFloat() else RATING_UNKNOWN
		}

		return Manga(
			id = generateUid(id.toLong()),
			title = name,
			url = "/obras/$slug",
			publicUrl = "https://$domain/obras/$slug",
			coverUrl = coverUrl,
			source = source,
			rating = rating,
			altTitles = emptySet(),
			contentRating = if (isNsfw) ContentRating.ADULT else ContentRating.SAFE,
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			largeCoverUrl = null,
			description = null,
			chapters = null,
		)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val mangaSlug = manga.url.substringAfter("/obras/")
		val mangaJson = webClient.httpGet("$apiUrl/obras/$mangaSlug", apiHeaders()).parseJson()
		val obraId = mangaJson.getInt("obr_id")
		val scanId = mangaJson.optInt("scan_id", defaultScanId).takeIf { it > 0 } ?: defaultScanId

		val description = mangaJson.optString("obr_descricao")
			.replace(Regex("</?strong>"), "")
			.replace("\\/", "/")
			.replace(Regex("\\s+"), " ")
			.trim()

		val status = mangaJson.optJSONObject("status")
			?.optString("stt_nome")
			?.let { parseStatus(it) }

		val tags = mangaJson.optJSONArray("tags")?.mapJSONNotNull { tagJson ->
			parseTag(tagJson)
		}?.toSet() ?: emptySet()

		val chapters = mangaJson.optJSONArray("capitulos")?.mapJSON { chapterJson ->
			parseChapter(chapterJson, obraId, scanId)
		} ?: emptyList()

		return manga.copy(
			title = mangaJson.optString("obr_nome", manga.title),
			description = description,
			state = status,
			tags = tags,
			chapters = chapters,
		)
	}

	private fun parseChapter(json: JSONObject, obraId: Int, scanId: Int): MangaChapter {
		val chapterId = json.getInt("cap_id")
		val chapterName = json.getString("cap_nome")
		val chapterNumber = json.optDouble("cap_numero").toFloat()
		val uploadDate = parseIsoDate(json.optString("cap_criado_em").ifBlank { json.optString("cap_liberar_em") })

		return MangaChapter(
			id = generateUid(chapterId.toLong()),
			title = chapterName,
			number = chapterNumber,
			url = "/capitulo/$chapterId/$obraId/$scanId",
			uploadDate = uploadDate,
			source = source,
			volume = 0,
			scanlator = null,
			branch = null,
		)
	}

	private fun parseStatus(status: String): MangaState? = when (status.lowercase()) {
		"ativo", "em andamento" -> MangaState.ONGOING
		"completo", "concluído" -> MangaState.FINISHED
		"hiato", "pausado" -> MangaState.PAUSED
		"cancelado", "abandonado" -> MangaState.ABANDONED
		else -> null
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val urlParts = chapter.url.removePrefix("/").removePrefix("capitulo/").split("/").filter { it.isNotBlank() }
		val chapterId = urlParts.getOrNull(0)?.takeIf { it.isNotBlank() }
			?: throw ParseException("URL de capítulo inválida", chapter.url)
		val obraId = urlParts.getOrNull(1)?.toIntOrNull() ?: 0
		val scanId = urlParts.getOrNull(2)?.toIntOrNull() ?: defaultScanId

		if (obraId <= 0) {
			throw ParseException(
				"Atualize os capítulos: abra os detalhes da obra de novo na Verdinha.",
				chapter.url,
			)
		}

		val chapterData = runCatching {
			webClient.httpGet("$apiUrl/capitulos/$chapterId", apiHeaders(scanId)).parseJson()
		}.getOrElse { error ->
			val message = error.message.orEmpty()
			if (message.contains("403") || message.contains("autenticado", ignoreCase = true) || message.contains("VIP", ignoreCase = true)) {
				val probed = probeChapterPages(scanId, obraId, chapter.number)
				if (probed.isNotEmpty()) return probed
				throw AuthRequiredException(source, error)
			}
			null
		}

		if (chapterData != null) {
			val capNumero = chapterData.optDouble("cap_numero").toFloat()
			val resolvedScanId = chapterData.optJSONObject("obra")?.optInt("scan_id", scanId)?.takeIf { it > 0 }
				?: scanId
			val resolvedObraId = chapterData.optJSONObject("obra")?.optInt("obr_id", obraId) ?: obraId
			val pagesArray = chapterData.optJSONArray("cap_paginas")
			if (pagesArray != null && pagesArray.length() > 0) {
				val pages = pagesArray.mapJSONNotNull { pageJson ->
					parsePageFromJson(pageJson, resolvedObraId, capNumero, resolvedScanId)
				}
				if (pages.isNotEmpty()) return pages
			}
		}

		val probed = probeChapterPages(scanId, obraId, chapter.number)
		if (probed.isNotEmpty()) {
			return probed
		}

		throw ParseException(
			if (resolveAccessToken() == null) {
				"Faça login na Verdinha pelo navegador do app (conta VIP) para ler os capítulos."
			} else {
				"Não foi possível carregar as páginas do capítulo. A leitura na Verdinha exige VIP ativo."
			},
			chapter.url,
		)
	}

	private fun parsePageFromJson(
		pageJson: JSONObject,
		obraId: Int,
		capNumero: Float,
		scanId: Int,
	): MangaPage? {
		val pagePath = pageJson.optString("path")
		val pageSrc = pageJson.optString("src")
		if (pagePath.isEmpty() && pageSrc.isEmpty()) return null
		val imageUrl = resolvePageUrl(pagePath, pageSrc, obraId, capNumero, scanId)
		if (!imageUrl.contains('.') || imageUrl.endsWith('/')) return null
		return MangaPage(
			id = generateUid(imageUrl),
			url = imageUrl,
			source = source,
			preview = null,
		)
	}

	private fun resolvePageUrl(
		path: String,
		src: String,
		obraId: Int,
		capNumero: Float,
		scanId: Int,
	): String {
		val folder = chapterFolderName(capNumero)
		return when {
			path.startsWith("http") -> path
			path.isNotEmpty() -> "$cdnUrl/$path"
			src.startsWith("http") -> src
			src.startsWith("manga_") -> "$cdnUrl/wp-content/uploads/WP-manga/data/$src"
			src.startsWith("wp-content") -> "$cdnUrl/$src"
			obraId > 0 -> "$cdnUrl/scans/$scanId/obras/$obraId/capitulos/$folder/$src"
			else -> "$cdnUrl/$src"
		}
	}

	private suspend fun probeChapterPages(scanId: Int, obraId: Int, chapterNumber: Float): List<MangaPage> {
		if (obraId <= 0) return emptyList()
		val folder = chapterFolderName(chapterNumber)
		val base = "$cdnUrl/scans/$scanId/obras/$obraId/capitulos/$folder/"
		val result = ArrayList<MangaPage>()
		var consecutiveMiss = 0
		// Arquivos no CDN começam em 001.jpg, não 000.jpg
		var pageIndex = 1
		while (pageIndex <= 300 && consecutiveMiss < 3) {
			val pageName = pageIndex.toString().padStart(3, '0')
			var found = false
			for (ext in PAGE_EXTENSIONS) {
				val url = "$base$pageName.$ext"
				if (imageExists(url)) {
					result.add(
						MangaPage(
							id = generateUid(url),
							url = url,
							source = source,
							preview = null,
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

	private suspend fun imageExists(url: String): Boolean = runCatching {
		webClient.httpHead(url.toHttpUrl()).use { it.isSuccessful }
	}.getOrDefault(false)

	private companion object {
		val PAGE_EXTENSIONS = arrayOf("jpg", "jpeg", "webp", "png")
	}

	private fun chapterFolderName(number: Float): String {
		val intValue = number.toInt()
		return if (number == intValue.toFloat()) {
			intValue.toString()
		} else {
			number.toString().replace('.', '_')
		}
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		val response = webClient.httpGet("$apiUrl/tags?limite=500", apiHeaders()).parseJson()
		val tagsArray = response.optJSONArray("resultados")
			?: response.optJSONArray("tags")
			?: return emptySet()
		return tagsArray.mapJSONNotNull { tagJson ->
			parseTag(tagJson)
		}.toSet()
	}

	private fun parseTag(tagJson: JSONObject): MangaTag? {
		val nested = tagJson.optJSONObject("tag") ?: tagJson
		val tagName = nested.optString("tag_nome").ifEmpty {
			nested.optString("nome", "")
		}.ifEmpty {
			tagJson.optString("tag_nome").ifEmpty { tagJson.optString("nome", "") }
		}
		if (tagName.isBlank()) return null
		val tagId = nested.optInt("tag_id").takeIf { it != 0 }
			?: nested.optInt("id", 0).takeIf { it != 0 }
			?: tagJson.optInt("tag_id").takeIf { it != 0 }
			?: tagJson.optInt("id", 0)
		return MangaTag(
			key = tagId.toString(),
			title = tagName.toTitleCase(),
			source = source,
		)
	}

	private fun parseIsoDate(raw: String?): Long {
		val value = raw?.trim().orEmpty()
		if (value.isEmpty() || value == "null") return 0L
		for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd HH:mm:ss")) {
			val parsed = SimpleDateFormat(pattern, Locale.US).apply {
				if (pattern.contains("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
			}.parseSafe(value)
			if (parsed > 0L) return parsed
		}
		return 0L
	}
}
