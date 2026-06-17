package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("ARTHUR_SCAN", "ArthurScan", "pt")
internal class ArthurScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ARTHUR_SCAN, "arthurscan.xyz")
