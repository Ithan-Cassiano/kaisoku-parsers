package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGA_MANHUA", "MangaManhua", "en")
internal class MangaManhua(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA_MANHUA, "mangaonlineteam.com", pageSize = 10)
