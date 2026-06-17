package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("MANGA18XYZ", "Manga18.xyz", "en", ContentType.HENTAI)
internal class Manga18Xyz(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MANGA18XYZ, "manga18.xyz", 36)
