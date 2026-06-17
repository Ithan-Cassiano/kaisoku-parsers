package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("KENHUAV2SCANK", "Kenhuav2Scan", "es")
internal class Kenhuav2Scan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KENHUAV2SCANK, "kenhuav2scan.com")
