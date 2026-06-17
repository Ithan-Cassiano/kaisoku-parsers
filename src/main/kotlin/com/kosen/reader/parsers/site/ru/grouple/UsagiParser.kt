package com.kosen.reader.parsers.site.ru.grouple

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.model.MangaParserSource

@MangaSourceParser("USAGI", "Usagi", "ru")
internal class UsagiParser(
	context: MangaLoaderContext,
) : GroupleParser(context, MangaParserSource.USAGI, 1) {

	override val configKeyDomain = ConfigKey.Domain(*domains)

	override fun getRequestHeaders() = super.getRequestHeaders().newBuilder()
		.add("referer", "https://$domain/")
		.build()

	companion object {

		val domains = arrayOf("web.usagi.one")
	}
}
