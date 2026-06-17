package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("INFAMOUSSCANS", "InfamousScans", "en")
internal class InfamousScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.INFAMOUSSCANS, "infamous-scans.com", 10)
