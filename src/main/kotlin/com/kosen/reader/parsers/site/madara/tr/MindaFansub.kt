package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@MangaSourceParser("MINDAFANSUB", "MindaFansub", "tr", ContentType.HENTAI)
internal class MindaFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MINDAFANSUB, "mindafansub.online")
