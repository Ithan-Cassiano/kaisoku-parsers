package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("DEXHENTAI", "DexHentai", "en", ContentType.HENTAI)
internal class DexHentai(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.DEXHENTAI, "dexhentai.com", 40, 36)
