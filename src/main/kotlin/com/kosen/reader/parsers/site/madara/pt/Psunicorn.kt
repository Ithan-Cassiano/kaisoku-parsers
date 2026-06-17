package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("PSUNICORN", "PsUnicorn", "pt", ContentType.HENTAI)
internal class Psunicorn(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PSUNICORN, "psunicorn.com")
