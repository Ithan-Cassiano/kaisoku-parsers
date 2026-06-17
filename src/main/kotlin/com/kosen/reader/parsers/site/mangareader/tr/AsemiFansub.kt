package com.kosen.reader.parsers.site.mangareader.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("ASEMIFANSUB", "AsemiFansub", "tr")
internal class AsemiFansub(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.ASEMIFANSUB, "asemifansub.com", pageSize = 20, searchPageSize = 10)
