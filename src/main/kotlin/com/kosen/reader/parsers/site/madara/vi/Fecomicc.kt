package com.kosen.reader.parsers.site.madara.vi

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.Broken

@Broken("No longer available")
@MangaSourceParser("FECOMICC", "Fecomic", "vi")
internal class Fecomicc(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.FECOMICC, "mangasup.net", 9) {
	override val listUrl = "comic/"
	override val tagPrefix = "the-loai/"
	override val datePattern = "dd/MM/yyyy"
}
