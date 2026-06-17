package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MANGA_KOMI", "MangaKomi", "en")
internal class MangaKomi(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA_KOMI, "mangakomi.io", pageSize = 18) {
	override val datePattern = "MMMM dd, yyyy"
}
