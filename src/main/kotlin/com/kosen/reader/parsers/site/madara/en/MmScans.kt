package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MMSCANS", "MmScans", "en")
internal class MmScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MMSCANS, "mm-scans.org") {
	override val selectChapter = "li.chapter-li"
	override val selectDesc = "div.summary-text"
	override val withoutAjax = true
}
