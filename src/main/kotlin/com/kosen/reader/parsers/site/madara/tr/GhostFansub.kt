package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser

@Broken("Redirect to @GRIMELEK")
@MangaSourceParser("GHOSTFANSUB", "GhostFansub", "tr")
internal class GhostFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.GHOSTFANSUB, "ghostfansub.co", 18)
// you now need to log in to access content
