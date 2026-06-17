package com.kosen.reader.parsers.site.manga18.zh

import org.jsoup.nodes.Document
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaListFilterOptions
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.manga18.Manga18Parser
import com.kosen.reader.parsers.util.attrAsRelativeUrl
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.mapChapters
import com.kosen.reader.parsers.util.selectFirstOrThrow

@MangaSourceParser("HANMAN18", "Hanman18", "zh", ContentType.HENTAI)
internal class Hanman18(context: MangaLoaderContext) :
	Manga18Parser(context, MangaParserSource.HANMAN18, "hanman18.com") {

	override suspend fun getFilterOptions() = MangaListFilterOptions(
		availableTags = emptySet(),
	)

	override suspend fun getChapters(doc: Document): List<MangaChapter> {
		return doc.body().select(selectChapter).mapChapters(reversed = true) { i, li ->
			val a = li.selectFirstOrThrow("a")
			val href = a.attrAsRelativeUrl("href")
			MangaChapter(
				id = generateUid(href),
				title = a.text(),
				number = i + 1f,
				volume = 0,
				url = href,
				uploadDate = 0,
				source = source,
				scanlator = null,
				branch = null,
			)
		}
	}
}
