package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("PONYMANGA", "PonyManga", "en", ContentType.HENTAI)
internal class PonyManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PONYMANGA, "ponymanga.com", 10)
