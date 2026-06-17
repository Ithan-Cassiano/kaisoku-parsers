package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("CRYSTALSCAN", "CrystalComics", "pt")
internal class CrystalScan(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.CRYSTALSCAN, "crystalcomics.com")

