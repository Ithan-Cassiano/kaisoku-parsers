package com.kosen.reader.parsers.site.gallery.vi

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.site.gallery.GalleryParser

@MangaSourceParser("BUONDUA", "Buon Dua", "vi", type = ContentType.OTHER)
internal class BuonDua(context: MangaLoaderContext) :
    GalleryParser(context, MangaParserSource.BUONDUA, "buondua.com") {

    override val configKeyDomain: ConfigKey.Domain = ConfigKey.Domain(
        "buondua.com",
        "buondua.us",
    )
}