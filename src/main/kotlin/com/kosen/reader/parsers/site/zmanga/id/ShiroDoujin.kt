package com.kosen.reader.parsers.site.zmanga.id

import org.jsoup.nodes.Document
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zmanga.ZMangaParser
import com.kosen.reader.parsers.util.attrAsRelativeUrl
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.mapChapters
import com.kosen.reader.parsers.util.selectFirstOrThrow
import java.text.SimpleDateFormat

@MangaSourceParser("SHIRO_DOUJIN", "ShiroDoujin", "id", ContentType.HENTAI)
internal class ShiroDoujin(context: MangaLoaderContext) :
	ZMangaParser(context, MangaParserSource.SHIRO_DOUJIN, "shirodoujin.com") {

	override suspend fun getChapters(doc: Document): List<MangaChapter> {
		val dateFormat = SimpleDateFormat(datePattern, sourceLocale)
		return doc.body().select(selectChapter).mapChapters(reversed = true) { i, li ->
			val a = li.selectFirstOrThrow("a")
			val href = a.attrAsRelativeUrl("href")
			val dateText = li.selectFirst(selectDate)?.text()
			val numChapter = li.selectFirstOrThrow(".flexch-infoz span").html().substringAfterLast("Chapter ")
				.substringBefore("<span")
			MangaChapter(
				id = generateUid(href),
				title = null,
				number = i + 1f,
				volume = 0,
				url = href,
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
