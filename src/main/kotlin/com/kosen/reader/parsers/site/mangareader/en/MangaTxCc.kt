package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("MANGATX_CC", "MangaTx.cc", "en")
internal class MangaTxCc(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANGATX_CC, "mangatx.cc", 30, 21) {
	override val datePattern = "dd-MM-yyyy"
	override val listUrl = "/manga-list"
}
