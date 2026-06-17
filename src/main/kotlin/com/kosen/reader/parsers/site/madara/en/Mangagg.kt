package com.kosen.reader.parsers.site.madara.en

import org.jsoup.nodes.Document
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.attrAsRelativeUrl
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.removeSuffix
import com.kosen.reader.parsers.util.selectFirstOrThrow
import com.kosen.reader.parsers.util.toAbsoluteUrl
import java.text.SimpleDateFormat

@MangaSourceParser("MANGAGG", "MangaGg", "en")
internal class Mangagg(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAGG, "mangagg.com") {
	override val tagPrefix = "genre/"
	override val datePattern = "MM/dd/yyyy"

	override suspend fun loadChapters(mangaUrl: String, document: Document): List<MangaChapter> {
		val chapters = mutableListOf<MangaChapter>()
		var page = 1

		while (true) {
			val url = mangaUrl.toAbsoluteUrl(domain).removeSuffix('/') + "/ajax/chapters/?t=$page"
			val doc = webClient.httpPost(url, emptyMap()).parseHtml()
			val pageChapters = parseChapterPage(doc)

			if (pageChapters.isEmpty()) {
				break
			}

			chapters.addAll(pageChapters)
			page++
		}

		return chapters.reversed().mapIndexed { index, chapter ->
			chapter.copy(number = index + 1f)
		}
	}

	private fun parseChapterPage(doc: Document): List<MangaChapter> {
		val dateFormat = SimpleDateFormat(datePattern, sourceLocale)
		return doc.select(selectChapter).map { li ->
			val a = li.selectFirstOrThrow("a")
			val href = a.attrAsRelativeUrl("href")
			val link = href + stylePage
			val dateText = li.selectFirst("a.c-new-tag")?.attr("title") ?: li.selectFirst(selectDate)?.text()
			val name = a.selectFirst("p")?.text() ?: a.ownText()
			MangaChapter(
				id = generateUid(href),
				title = name,
				number = 0f,
				volume = 0,
				url = link,
				uploadDate = parseChapterDate(
					dateFormat,
					dateText,
				),
				source = source,
				scanlator = null,
				branch = null,
			)
		}
	}
}
