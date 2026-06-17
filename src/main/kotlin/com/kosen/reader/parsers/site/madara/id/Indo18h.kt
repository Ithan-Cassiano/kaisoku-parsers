package com.kosen.reader.parsers.site.madara.id

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("INDO18H", "Indo18h", "id", ContentType.HENTAI)
internal class Indo18h(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.INDO18H, "indo18h.com", 24) {
	override val withoutAjax = true
}
