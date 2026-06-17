package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("NIGHTCOMIC", "Night Comic", "en")
internal class NightComic(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NIGHTCOMIC, "www.nightcomic.com") {
	override val postReq = true
}
