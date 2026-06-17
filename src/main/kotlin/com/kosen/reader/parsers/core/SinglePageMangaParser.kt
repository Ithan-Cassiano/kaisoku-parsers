package com.kosen.reader.parsers.core

import com.kosen.reader.parsers.InternalParsersApi
import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaListFilter
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.model.SortOrder

@InternalParsersApi
public abstract class SinglePageMangaParser(
	context: MangaLoaderContext,
	source: MangaParserSource,
) : AbstractMangaParser(context, source) {

	final override suspend fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
		if (offset > 0) {
			return emptyList()
		}
		return getList(order, filter)
	}

	public abstract suspend fun getList(order: SortOrder, filter: MangaListFilter): List<Manga>
}
