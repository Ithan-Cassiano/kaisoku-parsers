package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("HUNLIGHT", "HunLight", "en")
internal class HunLight(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HUNLIGHT, "hunlight.com")
