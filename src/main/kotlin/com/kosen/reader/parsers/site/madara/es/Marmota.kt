package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MARMOTA", "Marmota", "es", ContentType.COMICS)
internal class Marmota(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MARMOTA, "marmota.me", 48) {
	override val datePattern = "d 'de' MMMMM 'de' yyyy"
	override val tagPrefix = "genero/"
	override val listUrl = "comic/"
}
