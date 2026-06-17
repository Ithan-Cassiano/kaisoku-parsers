package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken
@MangaSourceParser("YDCOMICS", "YdComics", "en")
internal class YdComics(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.YDCOMICS, "yd-comics.com", pageSize = 20, searchPageSize = 10) {
	override val listUrl = "/index.php/series"
}
