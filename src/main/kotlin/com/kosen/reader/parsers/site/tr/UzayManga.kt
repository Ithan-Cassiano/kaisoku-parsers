package com.kosen.reader.parsers.site.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.uzaymanga.UzayMangaParser

@MangaSourceParser("UZAYMANGA", "Uzay Manga", "tr")
internal class UzayManga(context: MangaLoaderContext) :
	UzayMangaParser(
		context = context,
		source = MangaParserSource.UZAYMANGA,
		domain = "uzaymanga.com",
		cdnUrl = "https://uzaymangacdn3.efsaneler.can.re",
	)
