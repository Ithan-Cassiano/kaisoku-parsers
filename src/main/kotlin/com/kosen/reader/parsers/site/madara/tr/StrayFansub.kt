package com.kosen.reader.parsers.site.madara.tr

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.Broken

@Broken // The website is not functioning and displays the Plesk panel
@MangaSourceParser("STRAYFANSUB", "StrayFansub", "tr")
internal class StrayFansub(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.STRAYFANSUB, "strayfansub.com", 16) {
	override val tagPrefix = "seri-turu/"
}
