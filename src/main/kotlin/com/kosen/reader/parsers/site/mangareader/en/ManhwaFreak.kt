package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@Broken("Not dead, changed template")
@MangaSourceParser("MANHWA_FREAK", "ManhwaFreak", "en")
internal class ManhwaFreak(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.MANHWA_FREAK, "manhwafreak.xyz", pageSize = 30, searchPageSize = 42)
