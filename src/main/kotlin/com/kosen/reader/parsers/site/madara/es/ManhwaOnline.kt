package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.exception.ParseException
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.toAbsoluteUrl

@MangaSourceParser("MANHWAONLINE", "ManhwaOnline", "es")
internal class ManhwaOnline(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWAONLINE, "manhwa-online.com") {

	override val datePattern = "MMMM dd, yyyy"

	private val imageArrayRegex = Regex("""_d\s*=\s*\[(.*?)]\s*;""")
	private val xorKeyRegex = Regex("""return\(a\^(\d+)\)""")

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val fullUrl = chapter.url.toAbsoluteUrl(domain)
		val doc = webClient.httpGet(fullUrl).parseHtml()
		val scriptData = doc.getElementById("mowl-shield")
			?.data()
			?.takeIf { it.isNotBlank() }
			?: throw ParseException("Cannot find image decoder script", fullUrl)

		val xorKey = xorKeyRegex.find(scriptData)
			?.groupValues
			?.getOrNull(1)
			?.toIntOrNull()
			?: throw ParseException("Cannot extract xor key", fullUrl)

		val encodedArray = imageArrayRegex.find(scriptData)
			?.groupValues
			?.getOrNull(1)
			?: throw ParseException("Cannot extract encoded images array", fullUrl)

		val pages = encodedArray
			.split(',')
			.mapNotNull { token ->
				token.trim().removeSurrounding("\"").removeSurrounding("'").takeIf { it.isNotBlank() }
			}
			.mapIndexedNotNull { index, encoded ->
				val decodedUrl = runCatching {
					context.decodeBase64(encoded).map { byte ->
						((byte.toInt() and 0xFF) xor xorKey).toChar()
					}.joinToString("")
				}.getOrNull()?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null

				MangaPage(
					id = generateUid(decodedUrl),
					url = decodedUrl,
					preview = null,
					source = source,
				)
			}

		if (pages.isEmpty()) {
			throw ParseException("No pages extracted", fullUrl)
		}
		return pages
	}
}
