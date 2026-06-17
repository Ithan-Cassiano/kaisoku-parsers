package com.kosen.reader.parsers.site.madara.de

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGALESEN", "MangaLesen", "de")
internal class MangaLesen(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGALESEN, "mangalesen.net")
