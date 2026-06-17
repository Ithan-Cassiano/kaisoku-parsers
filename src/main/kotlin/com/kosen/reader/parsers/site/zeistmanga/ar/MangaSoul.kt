package com.kosen.reader.parsers.site.zeistmanga.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("MANGASOUL", "MangaSoul", "ar")
internal class MangaSoul(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.MANGASOUL, "www.manga-soul.com")
