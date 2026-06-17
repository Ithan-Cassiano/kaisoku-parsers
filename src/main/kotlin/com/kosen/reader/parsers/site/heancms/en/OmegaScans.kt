package com.kosen.reader.parsers.site.heancms.en

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.model.*
import com.kosen.reader.parsers.site.heancms.HeanCms

@MangaSourceParser("OMEGASCANS", "OmegaScans", "en", ContentType.HENTAI)
internal class OmegaScans(context: MangaLoaderContext) :
	HeanCms(context, MangaParserSource.OMEGASCANS, "omegascans.org")
