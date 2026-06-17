package com.kosen.reader.parsers.site.madara.vi

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("PINKTEACOMIC", "PinkTeaComic", "vi")
internal class PinkTeaComic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PINKTEACOMIC, "pinkteacomics.com") {
	override val datePattern = "d MMMM, yyyy"
}
