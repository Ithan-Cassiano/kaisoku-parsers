package com.kosen.reader.parsers.site.liliana.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.liliana.LilianaParser

@MangaSourceParser("MANHUAPLUSORG", "ManhuaPlus.org", "en")
internal class ManhuaPlusOrg(context: MangaLoaderContext) :
	LilianaParser(context, MangaParserSource.MANHUAPLUSORG, "manhuaplus.org")
