package com.kosen.reader.parsers.site.madara.es

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("RAGNAROKSCAN", "RagnarokScan", "es")
internal class RagnarokScan(context: MangaLoaderContext) :
    MadaraParser(context, MangaParserSource.RAGNAROKSCAN, "ragnarokscan.com") {
    override val stylePage = ""
    override val listUrl = "series/"
    override val tagPrefix = "genero/"
}
