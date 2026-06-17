package com.kosen.reader.parsers.site.mangareader.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.initmanga.InitMangaParser

@MangaSourceParser("MERLINSCANS", "MerlinScans", "tr")
internal class MerlinScans(context: MangaLoaderContext) :
	InitMangaParser(
		context = context,
		source = MangaParserSource.MERLINSCANS,
		domain = "merlintoon.com",
		pageSize = 20,
		searchPageSize = 20,
		latestUrlSlug = "son-guncellenenler",
	)
