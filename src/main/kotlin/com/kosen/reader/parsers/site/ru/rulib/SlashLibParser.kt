package com.kosen.reader.parsers.site.ru.rulib

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource

@MangaSourceParser("YAOILIB", "SlashLib", "ru")
internal class SlashLibParser(context: MangaLoaderContext) : LibSocialParser(
	context = context,
	source = MangaParserSource.YAOILIB,
	siteId = 2,
	siteDomains = arrayOf("v2.slashlib.me"),
)
