package com.kosen.reader.parsers.site.madara.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.madara.MadaraParser
import com.kosen.reader.parsers.Broken

@Broken("Original site closed")
@MangaSourceParser("STKISSMANGA_COM", "1stKissManga.com", "en")
internal class StkissMangaCom(context: MangaLoaderContext) :
	MadaraParser(context, MangaParserSource.STKISSMANGA_COM, "1stkissmanga.mom", 10) {
            override val postReq = true
      }
