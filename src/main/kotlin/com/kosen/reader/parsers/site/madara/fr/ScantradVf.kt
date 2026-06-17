package com.kosen.reader.parsers.site.madara.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("SCANTRADVF", "Scantrad-Vf", "fr")
internal class ScantradVf(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SCANTRADVF, "scantrad-vf.me") {
	override val datePattern = "d MMMM yyyy"
	override val tagPrefix = "genre/"
}
