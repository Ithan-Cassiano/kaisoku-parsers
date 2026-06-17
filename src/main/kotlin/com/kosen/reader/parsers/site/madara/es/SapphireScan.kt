package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("SAPPHIRESCAN", "SapphireScan", "es")
internal class SapphireScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SAPPHIRESCAN, "sapphirescan.com")
