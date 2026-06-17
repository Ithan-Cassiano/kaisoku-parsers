package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TRADUCCIONESAMISTOSAS", "TraduccionesAmistosas", "es")
internal class TraduccionesAmistosas(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TRADUCCIONESAMISTOSAS, "traduccionesamistosas.topmanhuas.org", 10) {
	override val datePattern = "d 'de' MMMMM 'de' yyyy"
}
