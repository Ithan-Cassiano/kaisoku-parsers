package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANHWAHENTAITO", "ManhwaHentai.to", "en", ContentType.HENTAI)
internal class ManhwaHentaiTo(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANHWAHENTAITO, "manhwahentai.to", 10) {
	override val tagPrefix = "pornhwa-genre/"
	override val listUrl = "pornhwa/"
	override val datePattern = "d MMMM yyyy"
}
