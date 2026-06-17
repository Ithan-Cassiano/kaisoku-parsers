package com.kosen.reader.site.natsu.id

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.site.natsu.NatsuParser

@MangaSourceParser("IKIRU", "Ikiru", "id")
internal class Ikiru(context: MangaLoaderContext) :
    NatsuParser(context, MangaParserSource.IKIRU, pageSize = 24) {

    override val configKeyDomain = ConfigKey.Domain("03.ikiru.wtf")

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        super.onCreateConfig(keys)
        keys.add(userAgentKey)
    }
}
