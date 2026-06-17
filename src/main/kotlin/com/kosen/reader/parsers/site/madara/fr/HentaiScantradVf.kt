package com.kosen.reader.parsers.site.madara.fr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("HENTAISCANTRADVF", "Hentai-Scantrad", "fr", ContentType.HENTAI)
internal class HentaiScantradVf(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HENTAISCANTRADVF, "hentai.scantrad-vf.cc") {
	override val datePattern = "d MMMM, yyyy"
}
