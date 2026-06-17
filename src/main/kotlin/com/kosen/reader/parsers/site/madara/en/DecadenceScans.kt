package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("DECADENCESCANS", "DecadenceScans", "en", ContentType.HENTAI)
internal class DecadenceScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.DECADENCESCANS, "reader.decadencescans.com", 10)
