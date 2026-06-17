package com.kosen.reader.parsers.site.madara.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGAROSE", "MangaRose", "ar")
internal class MangaRose(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGAROSE, "mangarose.net", pageSize = 20)
