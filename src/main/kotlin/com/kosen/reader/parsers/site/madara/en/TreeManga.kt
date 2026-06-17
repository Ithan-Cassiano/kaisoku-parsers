package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("TREE_MANGA", "TreeManga", "en")
internal class TreeManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TREE_MANGA, "treemanga.com") {
	override val datePattern = "MM/dd/yyyy"
}
