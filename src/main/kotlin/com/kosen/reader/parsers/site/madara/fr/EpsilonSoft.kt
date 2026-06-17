package com.kosen.reader.parsers.site.madara.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("EPSILONSOFT", "EpsilonSoft", "fr")
internal class EpsilonSoft(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.EPSILONSOFT, "beta.epsilonsoft.to") {
	override val datePattern = "dd/MM/yy"
	override val withoutAjax = true
}
