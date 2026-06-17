package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("STKISSMANGABLOG", "1StKissManga.net", "en")
internal class StkissMangaBlog(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.STKISSMANGABLOG, "1stkissmanga.org", 20) {
            override val postReq = true
      }
