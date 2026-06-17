package com.kosen.reader.parsers.site.liliana.ja

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.liliana.LilianaParser

@MangaSourceParser("MANGAKOMA01", "MangaKoma01", "ja")
internal class MangaKoma01(context: MangaLoaderContext) :
	LilianaParser(context, MangaParserSource.MANGAKOMA01, "mangakoma01.com")
