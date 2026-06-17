package com.kosen.reader.parsers.site.zeistmanga.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("MANHATOK", "ManhaTok", "ar")
internal class ManhaTok(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.MANHATOK, "manhatok.blogspot.com") {
	override val selectPage = "#seoneurons-target img"
}
