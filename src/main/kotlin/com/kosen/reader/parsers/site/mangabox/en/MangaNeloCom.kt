package com.kosen.reader.parsers.site.mangabox.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangabox.MangaboxParser
@Broken
@MangaSourceParser("MANGANELO_COM", "MangaNelo.com", "en")
internal class MangaNeloCom(context: MangaLoaderContext) :
	MangaboxParser(context, MangaParserSource.MANGANELO_COM) {
	override val configKeyDomain = ConfigKey.Domain("nelomanga.com", "m.manganelo.com", "chapmanganelo.com")
	override val otherDomain = "chapmanganelo.com"
}
