package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("SHOOTINGSTARSCANS", "Shooting Star Scans", "en")
internal class ShootingStarScans(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.SHOOTINGSTARSCANS, "shootingstarscans.com") {
	override val tagPrefix = "manga-tag/"
}
