package com.kosen.reader.parsers.site.natsu.id

import okhttp3.Headers
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.natsu.NatsuParser
import com.kosen.reader.parsers.util.attrAsRelativeUrl
import com.kosen.reader.parsers.util.generateUid
import com.kosen.reader.parsers.util.parseHtml

@MangaSourceParser("KIRYUU", "Kiryuu", "id")
internal class Kiryuu(context: MangaLoaderContext) :
    NatsuParser(context, MangaParserSource.KIRYUU, pageSize = 24) {

    override val configKeyDomain = ConfigKey.Domain("v5.kiryuu.to", "v1.kiryuu.to", "kiryuu03.com")

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(ConfigKey.DisableUpdateChecking(defaultValue = true))
    }

    override suspend fun loadChapters(
        mangaId: String,
        mangaAbsoluteUrl: String,
    ): List<MangaChapter> {
        val headers = Headers.headersOf(
            "hx-request", "true",
            "hx-target", "chapter-list",
            "hx-trigger", hxTrigger,
            "Referer", mangaAbsoluteUrl,
        )
        val url = "https://${domain}/wp-admin/admin-ajax.php?manga_id=$mangaId&page=1&action=chapter_list"
        val doc = webClient.httpGet(url, headers).parseHtml()

        return doc.select("div#chapter-list > div[data-chapter-number]").mapNotNull { element ->
            val a = element.selectFirst("a") ?: return@mapNotNull null
            val href = a.attrAsRelativeUrl("href")
            if (href.isBlank()) return@mapNotNull null

            MangaChapter(
                id = generateUid(href),
                title = element.selectFirst("div.font-medium span")?.text()?.trim().orEmpty(),
                url = href,
                number = element.attr("data-chapter-number").toFloatOrNull() ?: -1f,
                volume = 0,
                scanlator = null,
                uploadDate = parseDate(element.selectFirst("time")?.text()),
                branch = null,
                source = source,
            )
        }.reversed()
    }
}
