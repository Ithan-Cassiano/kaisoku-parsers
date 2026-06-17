package com.kosen.reader.parsers.site.ru.grouple

import com.kosen.reader.parsers.MangaLoaderContext
import com.kosen.reader.parsers.MangaSourceParser
import com.kosen.reader.parsers.config.ConfigKey
import com.kosen.reader.parsers.exception.AuthRequiredException
import com.kosen.reader.parsers.model.ContentType
import com.kosen.reader.parsers.model.MangaParserSource
import com.kosen.reader.parsers.util.*

@MangaSourceParser("ALLHENTAI", "AllHentai", "ru", type = ContentType.HENTAI)
internal class AllHentaiParser(
	context: MangaLoaderContext,
) : GroupleParser(context, MangaParserSource.ALLHENTAI, 1) {

	override val configKeyDomain = ConfigKey.Domain(
		"20.allhen.online",
		"z.ahen.me",
		"24.allhen.online",
		"2023.allhen.online",
	)

	override suspend fun isAuthorized(): Boolean =
		super.isAuthorized() || context.cookieJar.getCookies(domain).any { it.name == "remember_me" }

	override val authUrl: String
		get() {
			val targetUri = "https://$domain/".urlEncoded()
			return "https://qawa.org/internal/auth/login?targetUri=$targetUri&siteId=1"
		}

	override suspend fun getUsername(): String {
		val root = webClient.httpGet("https://qawa.org/").parseHtml().body()
		val element = root.selectFirst("img.user-avatar") ?: throw AuthRequiredException(source)
		val res = element.parent()?.text()
		return if (res.isNullOrEmpty()) {
			root.parseFailed("Cannot find username")
		} else {
			res
		}
	}
}
