package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("NVMANGA", "NvManga", "en")
internal class NvManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NVMANGA, "1manhwa.com") {
	override val datePattern = "dd/MM/yyyy"
	override val tagPrefix = "genre/"
	override val listUrl = "webtoon/"
}
