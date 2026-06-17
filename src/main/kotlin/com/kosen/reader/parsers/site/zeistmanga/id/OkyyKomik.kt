package com.kosen.reader.parsers.site.zeistmanga.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("OKYYKOMIK", "OkyyKomik", "id")
internal class OkyyKomik(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.OKYYKOMIK, "www.okyykomik.my.id")
