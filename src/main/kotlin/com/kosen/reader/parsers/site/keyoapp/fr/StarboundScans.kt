package com.kosen.reader.parsers.site.keyoapp.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.keyoapp.KeyoappParser

@Broken
@MangaSourceParser("STARBOUNDSCANS", "StarboundScans", "fr")
internal class StarboundScans(context: MangaLoaderContext) :
	KeyoappParser(context, MangaParserSource.STARBOUNDSCANS, "starboundscans.com")
