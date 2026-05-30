package org.koitharu.kotatsu.parsers.config

public interface MangaSourceConfig {

	public operator fun <T> get(key: ConfigKey<T>): T

	public operator fun <T> set(key: ConfigKey<T>, value: T) = Unit
}
