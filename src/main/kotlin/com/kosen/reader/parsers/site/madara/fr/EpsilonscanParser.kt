package com.kosen.reader.parsers.site.madara.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("EPSILONSCAN", "EpsilonScan", "fr", ContentType.HENTAI)
internal class EpsilonscanParser(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.EPSILONSCAN, "beta.epsilonscan.to") {
	override val datePattern = "dd/MM/yy"
	override val withoutAjax = true
}
