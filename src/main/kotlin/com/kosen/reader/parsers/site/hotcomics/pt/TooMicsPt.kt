package com.kosen.reader.parsers.site.hotcomics.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.hotcomics.HotComicsParser

@MangaSourceParser("TOOMICSPT", "TooMicsPt", "pt")
internal class TooMicsPt(context: MangaLoaderContext) :
	HotComicsParser(context, MangaParserSource.TOOMICSPT, "toomics.com/por") {
	override val isSearchSupported = false
	override val mangasUrl = "/webtoon/ranking/genre"
	override val selectMangas = "li > div.visual"
	override val selectMangaChapters = "li.normal_ep:has(.coin-type1)"
	override val selectTagsList = "div.genre_list li:not(.on) a"
	override val selectPages = "div[id^=load_image_] img"
	override val onePage = true
}
