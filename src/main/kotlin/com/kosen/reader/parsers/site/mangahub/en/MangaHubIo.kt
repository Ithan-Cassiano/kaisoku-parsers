package com.kosen.reader.parsers.site.mangahub.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangahub.MangaHubParser

@MangaSourceParser("MANGAHUB_IO", "MangaHub.io", "en")
internal class MangaHubIo(context: MangaLoaderContext) :
	MangaHubParser(context, MangaParserSource.MANGAHUB_IO, "mangahub.io", "m01")
