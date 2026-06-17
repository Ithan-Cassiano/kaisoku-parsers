package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import java.util.*

@MangaSourceParser("KALANGO", "Kalango", "pt")
internal class Kalango(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KALANGO, "kalango.org") {
	override val datePattern: String = "dd 'de' MMMM 'de' yyyy"
	override val sourceLocale: Locale = Locale.ENGLISH
}
