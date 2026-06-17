package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("VILLAINESSSCAN", "VillainessScan", "pt", ContentType.HENTAI)
internal class VillainessScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.VILLAINESSSCAN, "villainessscan.xyz", pageSize = 10) {
	override val datePattern: String = "dd 'de' MMMM 'de' yyyy"
}
