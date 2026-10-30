package com.parka.mdive.data.document

import com.parka.mdive.domain.document.ImageFailure
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import okhttp3.Authenticator
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request

/** Redirects share one deadline and every hop is validated before opening a connection. */
internal class ImageWebFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false)
        .cookieJar(CookieJar.NO_COOKIES).authenticator(Authenticator.NONE).proxyAuthenticator(Authenticator.NONE)
        .build(),
    private val timeoutMillis: Long = 20_000,
    private val userAgent: String = "MDive (https://github.com/HyeonSeong-P/MDive) okhttp/${okhttp3.OkHttp.VERSION}",
) {
    fun fetch(destination: String): ByteArray {
        var uri = checkedImageUrl(destination)
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        var redirects = 0
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) throw ImageProblem(ImageFailure.Timeout)
            val call = client.newCall(Request.Builder().url(uri.toString()).header("Accept", "image/*").header("User-Agent", userAgent).build())
            call.timeout().timeout(remaining, TimeUnit.NANOSECONDS)
            call.execute().use { response ->
                if (response.code in listOf(301, 302, 303, 307, 308)) {
                    if (redirects++ >= 5) throw ImageProblem(ImageFailure.Network)
                    val location = response.header("Location") ?: throw ImageProblem(ImageFailure.Network)
                    uri = checkedImageUrl(uri.resolve(location).toString())
                } else {
                    if (!response.isSuccessful) throw ImageProblem(ImageFailure.Network)
                    val body = response.body ?: throw ImageProblem(ImageFailure.Network)
                    val bytes = body.byteStream().use { stream -> readImageBytes(stream, body.contentLength()) {
                        if (System.nanoTime() >= deadline) { call.cancel(); throw ImageProblem(ImageFailure.Timeout) }
                    } }
                    return bytes
                }
            }
        }
    }
}
internal fun sha256(value: String): String = sha256(value.toByteArray())
internal fun sha256(value: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }
