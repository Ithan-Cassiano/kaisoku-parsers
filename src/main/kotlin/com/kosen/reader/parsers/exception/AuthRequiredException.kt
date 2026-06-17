package com.kosen.reader.parsers.exception

import okio.IOException
import com.kosen.reader.parsers.InternalParsersApi
import com.kosen.reader.parsers.model.MangaSource

/**
 * Authorization is required for access to the requested content
 */
public class AuthRequiredException @InternalParsersApi @JvmOverloads constructor(
	public val source: MangaSource,
	cause: Throwable? = null,
) : IOException("Authorization required", cause)
