package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("TEENMANHUA", "TeenManhua", "en")
internal class TeenManhua(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TEENMANHUA, "teenmanhua.com") {
	override val datePattern = "dd/MM/yyyy"
}
