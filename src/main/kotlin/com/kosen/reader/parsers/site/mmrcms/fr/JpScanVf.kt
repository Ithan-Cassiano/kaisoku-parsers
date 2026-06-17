package com.kosen.reader.parsers.site.mmrcms.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@Broken
@MangaSourceParser("JPSCANVF", "LireScanVf.com", "fr")
internal class JpScanVf(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.JPSCANVF, "lirescanvf.com") {
	override val sourceLocale: Locale = Locale.ENGLISH
}
