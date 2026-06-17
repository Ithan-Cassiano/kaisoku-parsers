package com.kosen.reader.parsers.site.madara.pt

import org.jsoup.nodes.Document
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.util.*
import java.text.SimpleDateFormat

@MangaSourceParser("MINITWOSCAN", "MiniTwoScan", "pt")
internal class MiniTwoScan(context: MangaLoaderContext) :
    MadaraParser(context, MangaParserSource.MINITWOSCAN, "minitwoscan.com") {

    override val withoutAjax = true
    override val postReq = true
}
