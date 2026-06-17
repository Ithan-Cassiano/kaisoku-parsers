package com.kosen.reader.parsers.site.madara.vi

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGAZODIAC", "MangaZodiac", "vi")
internal class MangaZodiac(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAZODIAC, "mangazodiac.com")
