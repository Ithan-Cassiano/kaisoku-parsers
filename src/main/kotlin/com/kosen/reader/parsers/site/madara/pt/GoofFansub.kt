package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("GOOFFANSUB", "GoofFansub", "pt", ContentType.HENTAI)
internal class GoofFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GOOFFANSUB, "gooffansub.com") {
	override val datePattern: String = "dd/MM/yyyy"
}
