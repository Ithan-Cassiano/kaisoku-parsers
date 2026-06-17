package com.kosen.reader.parsers.site.liliana.ja

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.liliana.LilianaParser

@MangaSourceParser("RAW1001", "Raw1001", "ja")
internal class Raw1001(context: MangaLoaderContext) :
	LilianaParser(context, MangaParserSource.RAW1001, "raw1001.net")
