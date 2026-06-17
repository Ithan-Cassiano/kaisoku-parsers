package com.kosen.reader.parsers.site.madara.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("HOTOON", "Holotoon", "id")
internal class Holotoon(context: MangaLoaderContext) :
    MadaraParser(context, MangaParserSource.HOTOON, "01.holotoon.site") {
    
    override val tagPrefix = "komik-genre/"
    override val listUrl = "komik/"
}
