package com.kosen.reader.parsers.site.hotcomics.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.hotcomics.HotComicsParser

@MangaSourceParser("TOOMICSEN", "TooMicsEn", "en")
internal class TooMicsEn(context: MangaLoaderContext) :
	HotComicsParser(context, MangaParserSource.TOOMICSEN, "toomics.com/en") {
	override val isSearchSupported = false
	override val mangasUrl = "/webtoon/ranking/genre"
	override val selectMangas = "li > div.visual"
	override val selectMangaChapters = "li.normal_ep:has(.coin-type1)"
	override val selectTagsList = "div.genre_list li:not(.on) a"
	override val selectPages = "div[id^=load_image_] img"
	override val onePage = true
}
