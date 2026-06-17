package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser
import com.kosen.reader.parsers.Broken

@Broken
@MangaSourceParser("DRAKESCANS", "DrakeComic", "en")
internal class DrakeScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.DRAKESCANS, "drakecomic.org", 20, 10)
