package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("SCAMBERTRASLATOR", "ScamberTraslator", "es")
internal class Scambertraslator(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SCAMBERTRASLATOR, "scambertraslator.com") {
	override val datePattern = "dd/MM/yyyy"
}
