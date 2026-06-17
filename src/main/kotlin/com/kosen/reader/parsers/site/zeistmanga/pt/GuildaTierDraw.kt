package com.kosen.reader.parsers.site.zeistmanga.pt

import com.kosen.reader.parsers.Broken
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.site.zeistmanga.ZeistMangaParser
import com.kosen.reader.parsers.util.mapToSet
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.requireElementById

@Broken
@MangaSourceParser("GUILDATIERDRAW", "GuildaTierDraw", "pt")
internal class GuildaTierDraw(context: MangaLoaderContext) :
	ZeistMangaParser(context, MangaParserSource.GUILDATIERDRAW, "www.guildatierdraw.top") {

	override suspend fun fetchAvailableTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain").parseHtml()
		return doc.requireElementById("LinkList2").select("ul li a").mapToSet {
			MangaTag(
				key = it.attr("href").substringBefore("?").substringAfterLast('/'),
				title = it.text(),
				source = source,
			)
		}
	}
}
