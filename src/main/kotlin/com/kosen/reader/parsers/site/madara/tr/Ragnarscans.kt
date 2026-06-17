package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.initmanga.InitMangaParser

@MangaSourceParser("RAGNARSCANS", "Ragnarscans", "tr")
internal class Ragnarscans(context: MangaLoaderContext) :
	InitMangaParser(
		context = context,
		source = MangaParserSource.RAGNARSCANS,
		domain = "ragnarscans.com",
		pageSize = 20,
		searchPageSize = 20,
		mangaUrlDirectory = "manga",
		popularUrlSlug = "en-cok-takip-edilenler",
		isCloudflareProtected = true,
	)
