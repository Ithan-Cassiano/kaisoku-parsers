package com.kosen.reader.parsers.site.madtheme.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madtheme.MadthemeParser

@MangaSourceParser("TOONILY_ME", "Toonily.Me", "en", ContentType.HENTAI)
internal class ToonilyMe(context: MangaLoaderContext) :
	MadthemeParser(context, MangaParserSource.TOONILY_ME, "toonily.me") {
	override val selectDesc = "div.summary div.section-body p.content"
}
