package com.kosen.reader.parsers.exception

import com.kosen.reader.parsers.InternalParsersApi

public class ParseException @InternalParsersApi @JvmOverloads constructor(
	public val shortMessage: String?,
	public val url: String,
	cause: Throwable? = null,
) : RuntimeException("$shortMessage at $url", cause)
