package com.praharsh.infotainmentlogconsole.data

import com.praharsh.infotainmentlogconsole.domain.LogLevel
import com.praharsh.infotainmentlogconsole.domain.LogSource
import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpLogRemoteDataSourceTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fetchAddsBearerAuthParsesJsonAndMarksRemoteSource() = runTest {
        val entry = SignalLogEntry(
            42,
            "2026-06-24 10:00:00",
            "NordicDrive",
            "Connectivity",
            "lteSignal",
            "-83dBm",
            LogLevel.INFO
        )
        val body = """{"entries":[${LogJsonCodec.parser.encodeToString(entry)}]}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val source = HttpLogRemoteDataSource(server.url("/").toString())

        val result = source.fetchLogs("NordicDrive", "session-token")

        assertEquals(LogSource.REMOTE, result.single().source)
        val request = server.takeRequest()
        assertEquals("Bearer session-token", request.getHeader("Authorization"))
        assertEquals("/v1/logs?brand=NordicDrive", request.path)
    }

    @Test
    fun nonSuccessfulResponseProducesBoundedError() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val source = HttpLogRemoteDataSource(server.url("/").toString())

        val error = runCatching {
            source.fetchLogs("NordicDrive", "expired")
        }.exceptionOrNull()

        assertTrue(error is LogSyncException)
        assertEquals("Log sync failed with HTTP 401.", error?.message)
    }

    @Test
    fun malformedJsonIsRejected() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))
        val source = HttpLogRemoteDataSource(server.url("/").toString())

        val error = runCatching {
            source.fetchLogs("NordicDrive", "session-token")
        }.exceptionOrNull()

        assertTrue(error is LogSyncException)
        assertEquals("Log sync returned invalid JSON.", error?.message)
    }

    @Test
    fun tokenWithHeaderInjectionCharactersIsRejectedBeforeRequest() = runTest {
        val source = HttpLogRemoteDataSource(server.url("/").toString())

        val error = runCatching {
            source.fetchLogs("NordicDrive", "token\nInjected: value")
        }.exceptionOrNull()

        assertTrue(error is LogSyncException)
        assertEquals("A valid session token is required.", error?.message)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun responseForWrongBrandIsRejected() = runTest {
        val entry = SignalLogEntry(
            44,
            "2026-06-24 10:00:00",
            "UrbanMotion",
            "Vehicle",
            "speedKph",
            "42",
            LogLevel.INFO
        )
        val body = """{"entries":[${LogJsonCodec.parser.encodeToString(entry)}]}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val source = HttpLogRemoteDataSource(server.url("/").toString())

        val error = runCatching {
            source.fetchLogs("NordicDrive", "session-token")
        }.exceptionOrNull()

        assertTrue(error is LogSyncException)
        assertEquals("Log sync returned an entry for the wrong brand.", error?.message)
    }
}
