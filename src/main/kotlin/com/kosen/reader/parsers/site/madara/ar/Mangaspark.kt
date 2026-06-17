package com.kosen.reader.parsers.site.madara.ar

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGASPARK", "Manga-Spark", "ar")
internal class Mangaspark(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGASPARK, "manga-spark.com", pageSize = 10) {
	override val postReq = true
	override val datePattern = "d MMMM، yyyy"
}
