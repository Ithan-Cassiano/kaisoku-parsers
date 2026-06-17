package com.kosen.reader.parsers.site.madara.ko

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import java.util.*

@MangaSourceParser("RAWDEX", "RawDex", "ko", ContentType.HENTAI)
internal class RawDex(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.RAWDEX, "rawdex.net", 40) {
	override val sourceLocale: Locale = Locale.ENGLISH
}
