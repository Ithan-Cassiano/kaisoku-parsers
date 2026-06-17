package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGA1K", "Manga1k", "en", ContentType.HENTAI)
internal class Manga1k(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA1K, "manga1k.com", 20) {
	override val withoutAjax = true
}
