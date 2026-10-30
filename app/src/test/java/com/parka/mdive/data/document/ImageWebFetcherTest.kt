package com.parka.mdive.data.document

import com.parka.mdive.domain.document.ImageFailure
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class ImageWebFetcherTest {
    private fun fixture(test: (MockWebServer, ImageWebFetcher) -> Unit) {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val client = OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager).followRedirects(false).followSslRedirects(false).build()
        val directory = Files.createTempDirectory("mdive-web").toFile()
        MockWebServer().use { server ->
            server.useHttps(serverTls.sslSocketFactory(), false)
            server.start()
            try { test(server, ImageWebFetcher(client, 500, "MDive/test (https://github.com/HyeonSeong-P/MDive) okhttp/4.12.0")) } finally { directory.deleteRecursively() }
        }
    }
    @Test fun httpsRedirectAndTransportNeverAddsCookieOrAuthorization() = fixture { server, fetcher ->
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/picture"))
        server.enqueue(MockResponse().setBody("first").setHeader("Set-Cookie", "secret=abc"))
        val url = server.url("/start").toString()
        assertEquals("first", fetcher.fetch(url).decodeToString())
        assertEquals(2, server.requestCount)
        server.enqueue(MockResponse().setBody("updated"))
        assertEquals("updated", fetcher.fetch(url).decodeToString())
        repeat(3) { val request = server.takeRequest(); assertNull(request.getHeader("Cookie")); assertNull(request.getHeader("Authorization")); assertEquals("MDive/test (https://github.com/HyeonSeong-P/MDive) okhttp/4.12.0", request.getHeader("User-Agent")) }
    }
    @Test fun redirectLimitAndDowngradeAreRejected() = fixture { server, fetcher ->
        repeat(5) { server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/allowed$it")) }
        server.enqueue(MockResponse().setBody("fifth redirect succeeds"))
        assertEquals("fifth redirect succeeds", fetcher.fetch(server.url("/boundary").toString()).decodeToString())
        assertEquals(6, server.requestCount)
        repeat(6) { server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/next$it")) }
        try { fetcher.fetch(server.url("/").toString()); fail() } catch (e: ImageProblem) { assertEquals(ImageFailure.Network, e.reason) }
        assertEquals(12, server.requestCount)
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://localhost/plain"))
        try { fetcher.fetch(server.url("/").toString()); fail() } catch (e: ImageProblem) { assertEquals(ImageFailure.Unsupported, e.reason) }
        assertEquals(13, server.requestCount)
    }
    @Test fun wholeRequestDeadlineStopsUnresponsiveResponse() = fixture { server, fetcher ->
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val start = System.nanoTime()
        try { fetcher.fetch(server.url("/").toString()); fail() } catch (_: java.io.InterruptedIOException) { }
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 2000)
    }
}
