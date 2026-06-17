package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("DUCKMANGA", "DuckManga", "en", ContentType.HENTAI)
internal class DuckManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.DUCKMANGA, "duckmanga.com", 20)
