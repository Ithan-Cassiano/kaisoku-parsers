package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAROLLS", "MangaRolls", "en")
internal class MangaRolls(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAROLLS, "mangarolls.net")
