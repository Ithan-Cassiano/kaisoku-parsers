package com.kosen.reader.parsers.site.madara.vi

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("TRUYENTUOITHO", "Truyện Tuổi Thơ", "vi")
internal class TruyenTuoiTho(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.TRUYENTUOITHO, "truyentuoitho.com") {
	override val datePattern = "dd/MM/yyyy"
	override val withoutAjax = true
}
