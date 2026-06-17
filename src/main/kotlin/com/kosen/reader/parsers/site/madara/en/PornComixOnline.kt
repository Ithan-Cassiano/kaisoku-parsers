package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("PORNCOMIXONLINE", "PornComix.online", "en", ContentType.HENTAI)
internal class PornComixOnline(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PORNCOMIXONLINE, "porncomix.online") {
	override val listUrl = "comic/"
	override val tagPrefix = "comic-genre/"
	override val stylePage = ""
}
