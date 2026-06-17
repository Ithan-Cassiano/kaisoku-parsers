package com.kosen.reader.parsers.site.zeistmanga.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser
import com.kosen.reader.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("ARABSDOUJIN", "ArabsDoujin", "ar", ContentType.HENTAI)
internal class ArabsDoujin(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.ARABSDOUJIN, "www.arabsdoujin.online")
