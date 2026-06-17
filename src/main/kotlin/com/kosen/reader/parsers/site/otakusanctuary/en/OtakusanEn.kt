package com.kosen.reader.parsers.site.otakusanctuary.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.otakusanctuary.OtakuSanctuaryParser
import com.kosen.reader.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("OTAKUSAN_EN", "Otaku Sanctuary (EN)", "en")
internal class OtakusanEn(context: MangaLoaderContext) :
	OtakuSanctuaryParser(context, MangaParserSource.OTAKUSAN_EN, "otakusan.me") {
	override val lang = "us"
	override val selectState = ".table-info tr:contains(Status) td"
}
