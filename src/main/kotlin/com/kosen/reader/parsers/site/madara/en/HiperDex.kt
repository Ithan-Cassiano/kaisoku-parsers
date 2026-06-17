package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.toAbsoluteUrl
import com.kosen.reader.parsers.util.toRelativeUrl

@MangaSourceParser("HIPERDEX", "HiperToon", "en", ContentType.HENTAI)
internal class HiperDex(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HIPERDEX, "hiperdex.com", 36) {

	override val listUrl = ""

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		// Remove ?style=list parameter from chapter URLs
		val absoluteUrl = chapter.url.toAbsoluteUrl(domain)
		val cleanUrl = if (absoluteUrl.contains("?style=list")) {
			absoluteUrl.replace("?style=list", "").replace("&style=list", "")
		} else {
			absoluteUrl
		}
		val relativeCleanUrl = cleanUrl.toRelativeUrl(domain)
		val modifiedChapter = chapter.copy(url = relativeCleanUrl)
		return super.getPages(modifiedChapter)
	}
}
