package com.kosen.reader.parsers.site.zeistmanga.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaPage
import com.kosen.reader.parsers.util.toAbsoluteUrl
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.selectFirstOrThrow

@MangaSourceParser("ULASCOMIC", "UlasComic", "id")
internal class UlasComic(context: MangaLoaderContext):
	ZeistMangaParser(context, MangaParserSource.ULASCOMIC, "www.ulascomic00.xyz") {
	
	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		val doc = webClient.httpGet(chapter.url.toAbsoluteUrl(domain)).parseHtml()
		return doc.selectFirstOrThrow("script:containsData(config['chapterImage'])")
			.data()
			.substringAfter("config['chapterImage'] = [")
			.substringBefore("];")
			.split("\",")
			.map { url ->
				val cleanUrl = url.trim().replace("\"", "")
				MangaPage(
					id = generateUid(cleanUrl),
					url = cleanUrl,
					preview = null,
					source = source,
				)
			}
	}
}
