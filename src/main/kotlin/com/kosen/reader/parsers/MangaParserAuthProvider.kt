package com.kosen.reader.parsers

import com.kosen.reader.parsers.exception.AuthRequiredException
import com.kosen.reader.parsers.exception.ParseException

/**
 * Implement this in your parser for authorization support
 */
public interface MangaParserAuthProvider {

	/**
	 * Return link to the login page, which will be opened in browser.
	 * Must be an absolute url
	 */
	public val authUrl: String

	/**
	 * Optional URL fetched (with session cookies) to verify login when DOM checks are inconclusive.
	 */
	public val authVerifyUrl: String?
		get() = null

	/**
	 * Quick check if user is logged in.
	 * In most case you should check for cookies in [MangaLoaderContext.cookieJar].
	 */
	public suspend fun isAuthorized(): Boolean

	/**
	 * Fetch and return current user`s name or login.
	 * Normally should not be called if [isAuthorized] returns false
	 * @throws [AuthRequiredException] if user is not logged in or authorization is expired
	 * @throws [ParseException] on parsing error
	 */
	public suspend fun getUsername(): String
}
