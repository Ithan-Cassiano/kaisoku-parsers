package com.kosen.reader.parsers.site.madara.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import java.util.*

@MangaSourceParser("MANHATIC", "Manhatic", "ar", ContentType.HENTAI)
internal class Manhatic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHATIC, "manhatic.com") {
	override val sourceLocale: Locale = Locale.ENGLISH
}
