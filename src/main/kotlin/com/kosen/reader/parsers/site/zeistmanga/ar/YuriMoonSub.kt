package com.kosen.reader.parsers.site.zeistmanga.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser
import com.kosen.reader.parsers.model.ContentType

@MangaSourceParser("YURIMOONSUB", "Yurimoonsub.blogspot.com", "ar", type = ContentType.HENTAI)
internal class YuriMoonSub(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.YURIMOONSUB, "yurimoonsub.blogspot.com")
