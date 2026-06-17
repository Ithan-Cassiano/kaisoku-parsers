package com.kosen.reader.parsers.site.mangareader.en

import org.jsoup.nodes.Element
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.util.attrAsRelativeUrlOrNull
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.nullIfEmpty
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.parseSafe
import com.kosen.reader.parsers.util.textOrNull
import com.kosen.reader.parsers.util.toAbsoluteUrl
import java.text.SimpleDateFormat
import java.util.Locale

@MangaSourceParser("GREEDSCANS", "Greed Scans", "en")
internal class GreedScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.GREEDSCANS, "greedscans.com", pageSize = 20, searchPageSize = 10) {

	override val detailsDescriptionSelector = "div.entry-content[itemprop=description], div.entry-content.entry-content-single"

	override suspend fun getDetails(manga: Manga): Manga {
		val docs = webClient.httpGet(manga.url.toAbsoluteUrl(domain)).parseHtml()
		val dateFormat = SimpleDateFormat(datePattern, Locale.US)
		val chapters = docs.select(selectChapter)
			.mapNotNull { element -> parseChapter(element, dateFormat) }
			.sortedWith(
				compareBy<MangaChapter> { it.number <= 0f }
					.thenBy { it.number }
					.thenBy { it.uploadDate.takeIf { date -> date > 0L } ?: Long.MAX_VALUE }
					.thenBy { it.title.orEmpty().lowercase(sourceLocale) },
			)

		return parseInfo(docs, manga, chapters)
	}

	private fun parseChapter(element: Element, dateFormat: SimpleDateFormat): MangaChapter? {
		val link = element.selectFirst("a[href]") ?: return null
		val relativeUrl = link.attrAsRelativeUrlOrNull("href") ?: return null
		val title = element.selectFirst(".chapternum")?.textOrNull()?.trim()
			?: link.attr("data-title").trim().nullIfEmpty()
			?: link.text().trim().nullIfEmpty()
		val number = element.attr("data-num").trim().toFloatOrNull()
			?: CHAPTER_NUMBER_REGEX.find(title.orEmpty())?.groupValues?.getOrNull(1)?.toFloatOrNull()
			?: 0f
		return MangaChapter(
			id = generateUid(relativeUrl),
			title = title,
			number = number,
			volume = 0,
			url = relativeUrl,
			scanlator = null,
			uploadDate = dateFormat.parseSafe(element.selectFirst(".chapterdate")?.text()?.trim()),
			branch = null,
			source = source,
		)
	}

	private companion object {
		private val CHAPTER_NUMBER_REGEX = Regex("""(\d+(?:\.\d+)?)""")
	}
}
