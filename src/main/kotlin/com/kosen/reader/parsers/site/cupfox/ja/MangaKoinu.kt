package com.kosen.reader.parsers.site.cupfox.ja

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.cupfox.CupFoxParser

@MangaSourceParser("MANGAKOINU", "MangaKoinu", "ja")
internal class MangaKoinu(context: MangaLoaderContext) :
	CupFoxParser(context, MangaParserSource.MANGAKOINU, "www.mangakoinu.com")
