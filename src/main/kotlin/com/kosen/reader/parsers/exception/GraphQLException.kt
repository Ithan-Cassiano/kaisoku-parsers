package com.kosen.reader.parsers.exception

import okio.IOException
import org.json.JSONArray
import com.kosen.reader.parsers.InternalParsersApi
import com.kosen.reader.parsers.util.json.mapJSONNotNull

public class GraphQLException @InternalParsersApi constructor(errors: JSONArray) : IOException() {

	public val messages: List<String> = errors.mapJSONNotNull {
		it.getString("message")
	}

	override val message: String
		get() = messages.joinToString("\n")
}
