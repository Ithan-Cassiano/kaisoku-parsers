package com.kosen.reader.parsers

import org.junit.jupiter.params.provider.EnumSource
import com.kosen.reader.parsers.model.MangaParserSource

// Change 'names' to test specified parsers
@EnumSource(MangaParserSource::class, names = [], mode = EnumSource.Mode.INCLUDE)
internal annotation class MangaSources
