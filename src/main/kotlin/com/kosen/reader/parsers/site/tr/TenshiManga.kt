package com.kosen.reader.parsers.site.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.uzaymanga.UzayMangaParser

@MangaSourceParser("TENSHIMANGA", "Tenshi Manga", "tr")
internal class TenshiManga(context: MangaLoaderContext) :
	UzayMangaParser(
		context = context,
		source = MangaParserSource.TENSHIMANGA,
		domain = "tenshimanga.com",
		cdnUrl = "https://tenshimangacdn4.efsaneler.can.re",
	)
