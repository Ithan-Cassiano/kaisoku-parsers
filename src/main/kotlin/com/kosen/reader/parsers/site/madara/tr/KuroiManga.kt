package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("KUROIMANGA", "KuroiManga", "tr", ContentType.HENTAI)
internal class KuroiManga(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.KUROIMANGA, "www.kuroimanga.com") {

	override val withoutAjax = true

	override val datePattern = "d MMMM yyyy"
}
