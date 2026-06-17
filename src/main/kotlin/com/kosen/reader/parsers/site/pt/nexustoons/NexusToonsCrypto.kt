package com.kosen.reader.parsers.site.pt.nexustoons

import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64

internal object NexusToonsCrypto {

	private const val SECRET = "OrionNexus2025CryptoKey!Secure"

	private val keys: Array<KeyState> by lazy {
		Array(5) { index ->
			val seed = "_orion_key_${index}_v2_$SECRET"
			val keyBytes = MessageDigest.getInstance("SHA-256")
				.digest(seed.toByteArray(Charsets.UTF_8))
			buildKeyState(keyBytes)
		}
	}

	fun decryptResponse(raw: JSONObject): JSONObject {
		if (!isEncryptedResponse(raw)) {
			return raw
		}
		return JSONObject(decryptPayload(raw))
	}

	fun decryptPayload(raw: JSONObject): String {
		val keyIndex = if (raw.optInt("v") == 1) 0 else raw.optInt("k")
		return decrypt(keyIndex, raw.getString("d"))
	}

	fun isEncryptedResponse(raw: JSONObject): Boolean {
		if (!raw.has("d") || !raw.has("k") || !raw.has("v")) {
			return false
		}
		val version = raw.optInt("v")
		return version == 1 || version == 2
	}

	private fun decrypt(keyIndex: Int, payload: String): String {
		require(keyIndex in keys.indices) { "Invalid key index: $keyIndex" }
		val state = keys[keyIndex]
		val input = Base64.getDecoder().decode(payload)
		val output = ByteArray(input.size)
		val key = state.key
		val rsbox = state.rsbox
		val keyLength = key.size
		for (c in input.indices.reversed()) {
			var value = input[c].toInt() and 0xFF
			val previous = if (c > 0) input[c - 1].toInt() and 0xFF else key[keyLength - 1].toInt() and 0xFF
			value = value xor previous
			value = rsbox[value].toInt() and 0xFF
			val rotation = ((key[(c + 3) % keyLength].toInt() and 0xFF) + (c and 0xFF) and 0xFF) % 7 + 1
			value = rotateRight(value, rotation)
			value = value xor (key[c % keyLength].toInt() and 0xFF)
			output[c] = value.toByte()
		}
		return String(output, Charsets.UTF_8)
	}

	private fun rotateRight(value: Int, rotation: Int): Int {
		val rot = rotation % 8
		return ((value ushr rot) or (value shl (8 - rot))) and 0xFF
	}

	private fun buildKeyState(keyBytes: ByteArray): KeyState {
		val sbox = IntArray(256) { it }
		var n = 0
		for (i in 0 until 256) {
			n = (n + sbox[i] + (keyBytes[i % keyBytes.size].toInt() and 0xFF)) % 256
			val tmp = sbox[i]
			sbox[i] = sbox[n]
			sbox[n] = tmp
		}
		val rsbox = ByteArray(256)
		for (i in 0 until 256) {
			rsbox[sbox[i]] = i.toByte()
		}
		return KeyState(keyBytes, rsbox)
	}

	private class KeyState(
		val key: ByteArray,
		val rsbox: ByteArray,
	)
}
