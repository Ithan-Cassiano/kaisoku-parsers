package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("RESETSCANS", "Resetscans", "en")
internal class ResetScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.RESETSCANS, "reset-scans.org", 20)

