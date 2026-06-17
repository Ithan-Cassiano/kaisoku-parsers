@file:JvmName("MangaParsersUtils")

package com.kosen.reader.parsers.util

import com.kosen.reader.parsers.model.MangaChapter
import com.kosen.reader.parsers.model.MangaListFilter
import kotlin.contracts.contract

public fun MangaListFilter?.isNullOrEmpty(): Boolean {
	contract {
		returns(false) implies (this@isNullOrEmpty != null)
	}
	return this == null || this.isEmpty()
}

public fun Collection<MangaChapter>.findById(chapterId: Long): MangaChapter? = find { x ->
	x.id == chapterId
}
