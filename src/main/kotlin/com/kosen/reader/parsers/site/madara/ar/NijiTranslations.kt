package com.kosen.reader.parsers.site.madara.ar

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.model.ContentType

@MangaSourceParser("NIJITRANSLATIONS", "Niji Translations", "ar", type = ContentType.HENTAI)
internal class NijiTranslations(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NIJITRANSLATIONS, "niji-translations.com") {
	override val postReq = true
}
