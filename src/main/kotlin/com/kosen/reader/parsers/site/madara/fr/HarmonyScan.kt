package com.kosen.reader.parsers.site.madara.fr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("HARMONYSCAN", "HarmonyScan", "fr")
internal class HarmonyScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.HARMONYSCAN, "harmony-scan.fr")
