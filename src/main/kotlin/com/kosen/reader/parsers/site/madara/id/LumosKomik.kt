package com.kosen.reader.parsers.site.madara.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import java.util.*

@MangaSourceParser("LUMOSKOMIK", "LumosKomik", "id")
internal class LumosKomik(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.LUMOSKOMIK, "lumos01.com") {
	override val tagPrefix = "genre/"
	override val listUrl = "komik/"
	override val datePattern = "dd MMMM yyyy"
	override val sourceLocale: Locale = Locale.ENGLISH
}
