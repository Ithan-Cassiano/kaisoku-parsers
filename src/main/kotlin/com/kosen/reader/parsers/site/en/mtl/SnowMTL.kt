package com.kosen.reader.parsers.site.en.mtl

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.ContentType

@Broken
@MangaSourceParser("SNOWMTL", "SnowMTL", "en", type = ContentType.OTHER)
internal class SnowMTL(context: MangaLoaderContext):
    MTLParser(context, source = MangaParserSource.SNOWMTL, "snowmtl.ru")
