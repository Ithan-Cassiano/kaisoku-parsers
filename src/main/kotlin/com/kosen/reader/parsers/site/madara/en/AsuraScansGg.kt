package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("ASURASCANSGG", "AsuraScansGg", "en")
internal class AsuraScansGg(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ASURASCANSGG, "asurascans.us")
