package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("ARTESSUPREMAS", "ArtesSupremas", "es")
internal class ArtesSupremas(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.ARTESSUPREMAS, "artessupremas.com") {
	override val datePattern = "dd/MM/yyyy"
}
