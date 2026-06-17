package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("DRAGONTRANSLATIONORG", "DragonTranslation.org", "es")
internal class DragonTranslationOrg(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.DRAGONTRANSLATIONORG, "dragontranslation.org", 16) {
	override val datePattern = "dd/MM/yyyy"
}
