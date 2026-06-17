package com.kosen.reader.parsers.site.madara.all

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import java.util.*

@Broken
@MangaSourceParser("EROMANHWA", "EroManhwa", "", ContentType.HENTAI)
internal class EroManhwa(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.EROMANHWA, "eromanhwa.org") {
	override val sourceLocale: Locale = Locale.ENGLISH
}
