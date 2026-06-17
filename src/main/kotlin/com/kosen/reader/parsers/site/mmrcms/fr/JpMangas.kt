package com.kosen.reader.parsers.site.mmrcms.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mmrcms.MmrcmsParser
import java.util.*

@Broken
@MangaSourceParser("JPMANGAS", "JpMangas", "fr")
internal class JpMangas(context: MangaLoaderContext) :
	MmrcmsParser(context, MangaParserSource.JPMANGAS, "jpmangas.xyz") {
	override val sourceLocale: Locale = Locale.ENGLISH
}
