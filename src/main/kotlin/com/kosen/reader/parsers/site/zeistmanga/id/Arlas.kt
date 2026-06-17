package com.kosen.reader.parsers.site.zeistmanga.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser

@MangaSourceParser("ARLAS", "Arlas", "id")
internal class Arlas(context: MangaLoaderContext) :
    ZeistMangaParser(context, com.kosen.reader.parsers.model.MangaParserSource.ARLAS, "arlas.my.id")
