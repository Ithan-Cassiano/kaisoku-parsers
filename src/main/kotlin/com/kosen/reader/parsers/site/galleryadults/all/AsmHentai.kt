package com.kosen.reader.parsers.site.galleryadults.all

import org.jsoup.nodes.Element
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.MangaTag
import com.kosen.reader.parsers.site.galleryadults.GalleryAdultsParser
import com.kosen.reader.parsers.util.mapToSet
import com.kosen.reader.parsers.util.removeSuffix
import java.util.*

@MangaSourceParser("ASMHENTAI", "AsmHentai", type = ContentType.HENTAI)
internal class AsmHentai(context: MangaLoaderContext) :
	GalleryAdultsParser(context, MangaParserSource.ASMHENTAI, "asmhentai.com") {

	override val selectGallery = ".preview_item"
	override val selectGalleryLink = ".image a"
	override val selectGalleryImg = ".image img"
	override val pathTagUrl = "/tags/?page="
	override val selectTags = ".tags_page ul.tags"
	override val selectAuthor = "div.tags:contains(Artists:) .tag_list a span.tag"
	override val idImg = "fimg"

	override suspend fun getFilterOptions() = super.getFilterOptions().copy(
		availableLocales = setOf(
			Locale.ENGLISH,
			Locale.JAPANESE,
			Locale.CHINESE,
			Locale("tr"),
		),
	)

	override fun Element.parseTags() = select("a").mapToSet {
		val key = it.attr("href").removeSuffix('/').substringAfterLast('/')
		val name = it.selectFirst(".tag")?.html()?.substringBefore("<") ?: it.html().substringBefore("<")
		MangaTag(
			key = key,
			title = name,
			source = source,
		)
	}
}
