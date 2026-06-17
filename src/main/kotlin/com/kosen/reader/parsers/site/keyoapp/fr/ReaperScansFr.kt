package com.kosen.reader.parsers.site.keyoapp.fr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.keyoapp.KeyoappParser

@Broken
@MangaSourceParser("REAPERSCANS_FR", "ReaperScans.fr", "fr")
internal class ReaperScansFr(context: MangaLoaderContext) :
	KeyoappParser(context, MangaParserSource.REAPERSCANS_FR, "reaper-scans.fr")
