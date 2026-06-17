package com.kosen.reader.parsers.site.madara.pt

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.mapNotNullToSet
import com.kosen.reader.parsers.util.parseHtml
import com.kosen.reader.parsers.util.toTitleCase

@MangaSourceParser("MRBENNE", "MrBenne", "pt", ContentType.HENTAI)
internal class MrBenne(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.MRBENNE, "mrbenne.com", 10) {
	override val datePattern: String = "dd/MM/yyyy"

	override suspend fun fetchAvailableTags(): Set<MangaTag> {
		val doc = webClient.httpGet("https://$domain/?s=&post_type=wp-manga").parseHtml()
		val body = doc.body()
		val root = body.selectFirst("div.form-group.checkbox-group")
		val list = root?.select("div.checkbox").orEmpty()
		val keySet = HashSet<String>(list.size)
		return list.mapNotNullToSet { div ->
			val input = div.selectFirst("input") ?: return@mapNotNullToSet null
			val label = div.selectFirst("label") ?: return@mapNotNullToSet null
			val tag = input.attr("value")
			if (tag.isEmpty() || !keySet.add(tag)) {
				return@mapNotNullToSet null
			}
			MangaTag(
				key = tag,
				title = label.ownText().ifEmpty {
					tag
				}.toTitleCase(sourceLocale),
				source = source,
			)
		}
	}
}
