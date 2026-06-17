package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("NOCSUMMER", "NocturneSummer", "pt", ContentType.HENTAI)
internal class Nocsummer(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NOCSUMMER, "nocfsb.com", 18) {
	override val datePattern = "dd 'de' MMMMM 'de' yyyy"
}
