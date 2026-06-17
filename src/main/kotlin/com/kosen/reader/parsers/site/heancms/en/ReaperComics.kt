package com.kosen.reader.parsers.site.heancms.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.site.heancms.HeanCms
import com.kosen.reader.parsers.Broken

@Broken("Closed site")
@MangaSourceParser("REAPERCOMICS", "ReaperComics", "en")
internal class ReaperComics(context: MangaLoaderContext) :
	HeanCms(context, MangaParserSource.REAPERCOMICS, "reaperscans.com") {
	override val cdn = "media.reaperscans.com/file/4SRBHm//"
	override val paramsUpdated = "updated_at"
	override val selectPages = ".flex > img"

	override fun reqUrl(seriesId: Long): String {
		return "https://$apiPath/chapters/$seriesId?page=1&perPage=9999&order=desc"
	}
}
