package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("PETROTECHSOCIETY", "Petrotech Society", "en", ContentType.HENTAI)
internal class PetrotechSociety(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.PETROTECHSOCIETY, "www.petrotechsociety.org", pageSize = 10) {
	override val postReq = true
}
