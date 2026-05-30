package org.koitharu.kotatsu.parsers

import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig

internal class SourceConfigMock : MangaSourceConfig {

	private val values = mutableMapOf<String, Any?>()

	@Suppress("UNCHECKED_CAST")
	override fun <T> get(key: ConfigKey<T>): T = values[key.key] as? T ?: key.defaultValue

	override fun <T> set(key: ConfigKey<T>, value: T) {
		values[key.key] = value
	}
}