package com.kosen.reader.parsers.util

import com.kosen.reader.parsers.model.Manga
import com.kosen.reader.parsers.model.MangaChapter

/**
 * Ordem de leitura: capítulo menor (0/1) → maior.
 * Usa número do capítulo quando disponível; caso contrário inverte listas tipicamente newest-first.
 */
fun List<MangaChapter>.sortedForReading(): List<MangaChapter> {
	if (size <= 1) {
		return this
	}
	val withPositiveNumber = count { it.number > 0f }
	if (withPositiveNumber >= (size + 1) / 2) {
		return sortedWith(
			compareBy(
				{ it.number },
				{ it.volume },
				{ it.uploadDate },
				{ it.id },
			),
		)
	}
	val firstNumber = first().number
	val lastNumber = last().number
	return when {
		firstNumber > lastNumber && firstNumber > 0f && lastNumber > 0f ->
			sortedWith(compareBy({ it.number }, { it.volume }, { it.uploadDate }, { it.id }))

		firstNumber in 0f..lastNumber -> this
		else -> asReversed()
	}
}

fun Manga.withChaptersSortedForReading(): Manga {
	val chapters = chapters ?: return this
	val sorted = chapters.sortedForReading()
	return if (sorted === chapters) this else copy(chapters = sorted)
}
