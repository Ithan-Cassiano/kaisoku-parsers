package com.kosen.reader.parsers.site.mangareader.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("SNOWMACHINETRANSLATION", "Snow Machine Translation", "en")

internal class SnowMachineTranslation(context: MangaLoaderContext) :
    MangaReaderParser(context, MangaParserSource.SNOWMACHINETRANSLATION, "snowmachinetranslation.com", pageSize = 24, searchPageSize = 10) {
    override val listUrl = "/manga"


}
