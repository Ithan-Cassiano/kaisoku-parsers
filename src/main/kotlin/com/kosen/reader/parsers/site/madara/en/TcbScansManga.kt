package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TCBSCANSMANGA", "TcbScansManga", "en")
internal class TcbScansManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TCBSCANSMANGA, "tcbscans-manga.com", 10) {
	override val selectPage = "img"
}
