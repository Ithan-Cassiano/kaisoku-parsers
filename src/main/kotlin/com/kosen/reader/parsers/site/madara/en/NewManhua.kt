package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken
@MangaSourceParser("NEWMANHUA", "NewManhua", "en")
internal class NewManhua(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.NEWMANHUA, "newmanhua.com", pageSize = 16)
