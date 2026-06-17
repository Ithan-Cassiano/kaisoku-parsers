package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("THEBLANK", "TheBlank", "en", ContentType.HENTAI)
internal class TheBlank(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.THEBLANK, "theblank.net") {
	override val datePattern = "dd/MM/yyyy"
}
