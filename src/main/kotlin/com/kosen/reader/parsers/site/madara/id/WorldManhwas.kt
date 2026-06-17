package com.kosen.reader.parsers.site.madara.id

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("WORLDMANHWAS", "WorldManhwas", "id", ContentType.HENTAI)
internal class WorldManhwas(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.WORLDMANHWAS, "worldmanhwas.zone", 10) {
	override val tagPrefix = "genre/"
	override val listUrl = "komik/"
	override val datePattern = "d MMMM yyyy"
}
