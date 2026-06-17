package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.Broken

@Broken // Host error
@MangaSourceParser("STICKHORSE", "StickHorse", "es")
internal class Stickhorse(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.STICKHORSE, "www.stickhorse.cl") {
	override val postReq = true
}
