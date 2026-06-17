package com.kosen.reader.parsers.site.madara.vi

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("RUAHAPCHANHDAY", "Rùa Hấp Chanh Dây", "vi")
internal class RuaHapChanhDay(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.RUAHAPCHANHDAY, "ruahapchanhday.com", 30) {
	override val datePattern = "dd/MM/yyyy"
}
