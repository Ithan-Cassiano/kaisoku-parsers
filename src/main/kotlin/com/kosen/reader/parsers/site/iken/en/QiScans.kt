package com.kosen.reader.parsers.site.iken.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.site.iken.IkenParser
import com.kosen.reader.parsers.util.*
import java.util.EnumSet

@MangaSourceParser("QISCANS", "Qi Scans", "en")
internal class QiScans(context: MangaLoaderContext) :
    IkenParser(context, MangaParserSource.QISCANS, "qimanhwa.com", 18, true) {

    override val availableSortOrders: Set<SortOrder> = EnumSet.of(
        SortOrder.POPULARITY,
        SortOrder.UPDATED,
    )

    override suspend fun getListPage(page: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val url = buildString {
            append("https://")
            append(defaultDomain)
            append("/api/query?page=")
            append(page)
            append("&perPage=18&searchTerm=")

            filter.query?.let {
                append(filter.query.urlEncoded())
            }

            if (filter.tags.isNotEmpty()) {
                append("&genreIds=")
                filter.tags.joinTo(this, ",") { it.key }
            }

            append("&seriesType=")
            filter.types.oneOrThrowIfMany()?.let {
                append(
                    when (it) {
                        ContentType.MANGA -> "MANGA"
                        ContentType.MANHWA -> "MANHWA"
                        ContentType.MANHUA -> "MANHUA"
                        ContentType.OTHER -> "RUSSIAN"
                        else -> ""
                    },
                )
            }

            append("&seriesStatus=")
            filter.states.oneOrThrowIfMany()?.let {
                append(
                    when (it) {
                        MangaState.ONGOING -> "ONGOING"
                        MangaState.FINISHED -> "COMPLETED"
                        MangaState.UPCOMING -> "COMING_SOON"
                        MangaState.ABANDONED -> "DROPPED"
                        else -> ""
                    },
                )
            }

            append("&orderBy=")
            append(
                when (order) {
                    SortOrder.POPULARITY -> "totalViews"
                    SortOrder.UPDATED -> "updatedAt"
                    else -> "totalViews"
                },
            )
        }
        return parseMangaList(webClient.httpGet(url).parseJson())
    }
}
