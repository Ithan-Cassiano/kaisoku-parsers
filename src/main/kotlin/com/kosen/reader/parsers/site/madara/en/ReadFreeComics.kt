package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.Broken

@Broken
@MangaSourceParser("READFREECOMICS", "ReadFreeComics", "en")
internal class ReadFreeComics(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.READFREECOMICS, "readfreecomics.com") {
	override val tagPrefix = "webtoon-comic-genre/"
	override val listUrl = "webtoon-comic/"
}
