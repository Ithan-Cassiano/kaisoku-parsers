package com.kosen.reader.parsers.site.pt

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.core.PagedMangaParser
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
import com.kosen.reader.parsers.util.json.mapJSON
import com.kosen.reader.parsers.util.json.mapJSONNotNull
import com.kosen.reader.parsers.util.oneOrThrowIfMany
import com.kosen.reader.parsers.util.parseJson
import com.kosen.reader.parsers.util.parseSafe
import com.kosen.reader.parsers.util.toTitleCase
import java.text.SimpleDateFormat
import java.util.EnumSet

@MangaSourceParser("MEDIOCRETOONS", "MediocreToons", "pt")
internal class MediocreToons(context: MangaLoaderContext) : PagedMangaParser(
	context,
	source = MangaParserSource.MEDIOCRETOONS,
	pageSize = 20,
) {
	override val configKeyDomain = ConfigKey.Domain("mediocretoons.com")
	private val apiUrl = "https://api.mediocretoons.site"
	private val cdnUrl = "https://cdn.mediocretoons.site"

	override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
		.add("Referer", "https://mediocretoons.com/")
		.add("X-App-Key", "toons-mediocre-app")
		.build()

	override val availableSortOrders: Set<SortOrder> = EnumSet.of(
		SortOrder.UPDATED,
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
				MangaState.ABANDONED,
			),
			availableContentTypes = EnumSet.of(
				ContentType.MANGA,
				ContentType.HENTAI,
			),
		)
	}

	private val chapterDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sourceLocale)

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {

        val url = when {
            // This part remains the same for handling searches and filters.
            !filter.query.isNullOrEmpty() || filter.tags.isNotEmpty() || filter.states.isNotEmpty() || filter.types.isNotEmpty() -> buildSearchUrl(
                page,
                filter,
            )

            // This is the modified block for the default "latest" or "recent" list.
            else -> {
                "$apiUrl/obras/recentes".toHttpUrl().newBuilder()
                    .addQueryParameter("limite", pageSize.toString()) // Or a fixed "20" if you prefer
                    .addQueryParameter("pagina", page.toString())
                    .addQueryParameter("formato", "5") // Added the new required parameter
                    .build()
            }
        }

        val response = webClient.httpGet(url).parseJson()
        val results = response.optJSONArray("data") ?: return emptyList()
        return results.mapJSON { parseMangaFromJson(it) }
    }

	private fun buildSearchUrl(page: Int, filter: MangaListFilter): HttpUrl {
		val builder = "$apiUrl/obras".toHttpUrl().newBuilder().addQueryParameter("limite", pageSize.toString())
			.addQueryParameter("pagina", page.toString())

		// Add search query
		if (!filter.query.isNullOrEmpty()) {
			builder.addQueryParameter("string", filter.query)
		}

		// Add tags
		filter.tags.forEach { tag ->
			builder.addQueryParameter("tags[]", tag.key)
		}

		if (filter.types.isNotEmpty()) {
			filter.types.forEach { contentType ->
				val formatId = when (contentType) {
					ContentType.MANGA -> "5"
					ContentType.HENTAI -> "8"
					else -> null
				}
				formatId?.let { builder.addQueryParameter("formato[]", it) }
			}
		}

		// Add status
		filter.states.oneOrThrowIfMany()?.let { state ->
			val statusId = when (state) {
				MangaState.ONGOING -> "2"
				MangaState.FINISHED -> "4"
				MangaState.PAUSED -> "3"
				else -> null
			}
			statusId?.let { builder.addQueryParameter("status", it) }
		}

		return builder.build()
	}

	private fun parseMangaFromJson(json: JSONObject): Manga {
		val id = json.getInt("id")
		val name = json.getString("nome")
		val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
		val coverPath = json.optString("imagem", "")

		val coverUrl = when {
			coverPath.startsWith("http") -> coverPath
			coverPath.isNotEmpty() -> "$cdnUrl/obras/$id/$coverPath"
			else -> ""
		}

		val rating = RATING_UNKNOWN

		return Manga(
			id = generateUid(id.toLong()),
			title = name,
			url = "/obra/$id/$slug",
			publicUrl = "https://$domain/obra/$id/$slug",
			coverUrl = coverUrl,
			source = source,
			rating = rating,
			altTitles = emptySet(),
			contentRating = null,
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			largeCoverUrl = null,
			description = null,
			chapters = null,
		)
	}

	override suspend fun getDetails(manga: Manga): Manga {
		val mangaId = manga.url.substringAfter("/obra/").substringBefore("/")
		val response = webClient.httpGet("$apiUrl/obras/$mangaId").parseJson()

		val description = response.optString("descricao").replace(Regex("</?[^>]+>"), "").replace("\\/", "/")
			.replace(Regex("\\s+"), " ").trim()

		val status = response.optJSONObject("status")?.let { statusObj ->
			when (statusObj.optString("nome").lowercase()) {
				"ativo", "em andamento" -> MangaState.ONGOING
				"concluído", "completo" -> MangaState.FINISHED
				"hiato" -> MangaState.PAUSED
				"cancelado" -> MangaState.ABANDONED
				else -> null
			}
		}

		val tags = response.optJSONArray("tags")?.mapJSON { tagJson ->
			MangaTag(
				key = tagJson.getInt("id").toString(),
				title = tagJson.getString("nome").toTitleCase(),
				source = source,
			)
		}?.toSet() ?: emptySet()

		val chapters = response.optJSONArray("capitulos")?.mapJSON { chapterJson ->
			parseChapter(chapterJson)
		}?.sortedBy { it.number } ?: emptyList()

		return manga.copy(
			title = response.optString("nome", manga.title),
			description = description,
			state = status,
			tags = tags,
			chapters = chapters,
		)
	}

	private fun parseChapter(json: JSONObject): MangaChapter {
		val chapterId = json.getInt("id")
		val chapterName = json.getString("nome")
		val chapterDate = json.optString("criado_em")

		val chapterNumber = json.optString("numero").toFloat()

		return MangaChapter(
			id = generateUid(chapterId.toLong()),
			title = "Capítulo $chapterName",
			number = chapterNumber,
			url = "/capitulo/$chapterId",
			uploadDate = chapterDateFormat.parseSafe(chapterDate),
			source = source,
			volume = 0,
			scanlator = null,
			branch = null,
		)
	}


	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val chapterId = chapter.url.substringAfter("/capitulo/")

		val response = webClient.httpGet("$apiUrl/capitulos/$chapterId").parseJson()

		val pagesArray = response.optJSONArray("paginas") ?: throw Exception("No pages found in chapter")

		val obraInfo = response.optJSONObject("obra") ?: throw Exception("Obra information not found")
		val obraId = obraInfo.getInt("id")
		val chapterNumber = response.optString("numero", "")

		return pagesArray.mapJSONNotNull { pageJson ->
			val pageSrc = pageJson.optString("src")

			if (pageSrc.isEmpty()) return@mapJSONNotNull null

			val imageUrl = when {
				pageSrc.startsWith("http") -> pageSrc
				else -> "$cdnUrl/obras/$obraId/capitulos/$chapterNumber/$pageSrc"
			}

			MangaPage(
				id = generateUid(imageUrl),
				url = imageUrl,
				source = source,
				preview = null,
			)
		}
	}

	private suspend fun fetchAvailableTags(): Set<MangaTag> {
		val url = "$apiUrl/tags"
		val body = webClient.httpGet(url).body?.string()?.trim()

		if (body == null) return emptySet()

		val tagsArray = if (body.startsWith("[")) {
			org.json.JSONArray(body)
		} else {
			val json = JSONObject(body)
			json.optJSONArray("tags") ?: json.optJSONArray("data") ?: org.json.JSONArray()
		}

		return tagsArray.mapJSONNotNull { tagJson ->
			MangaTag(
				key = tagJson.getInt("id").toString(),
				title = tagJson.getString("nome").toTitleCase(),
				source = source,
			)
		}.toSet()
	}
}
