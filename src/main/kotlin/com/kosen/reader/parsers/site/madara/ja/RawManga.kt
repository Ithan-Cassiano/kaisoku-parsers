package com.kosen.reader.parsers.site.madara.ja

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("RAWMANGA", "RawManga", "ja")
internal class RawManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.RAWMANGA, "rawmanga.su", 24) {
	override val listUrl = "r/"
	override val selectPage = "div.mg-item"
}
