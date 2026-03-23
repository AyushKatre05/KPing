package com.kping.scheduler.checkers

import com.kping.models.MonitorDto
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.security.cert.X509Certificate

class HttpChecker : MonitorChecker {
    private val client = HttpClient(CIO) {
        install(HttpTimeout)
        expectSuccess = false
    }

    private fun getSslExpiryDays(urlStr: String): Int? {
        if (!urlStr.startsWith("https://")) return null
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()
            val certs = conn.serverCertificates
            if (certs.isNotEmpty()) {
                val cert = certs[0] as X509Certificate
                val diff = cert.notAfter.time - System.currentTimeMillis()
                return (diff / (1000L * 60 * 60 * 24)).toInt()
            }
        } catch (e: Exception) { }
        return null
    }

    override suspend fun check(monitor: MonitorDto): CheckResult {
        var statusCode = 0
        var isUp = false
        var errorCause: String? = null
        var bodyString = ""
        var sslExpiry: Int? = null
        
        withContext(Dispatchers.IO) {
            sslExpiry = getSslExpiryDays(monitor.url)
        }

        val timeTaken = try {
            val start = System.currentTimeMillis()
            val response = client.request(monitor.url) {
                method = HttpMethod.parse(monitor.httpMethod)
                timeout { requestTimeoutMillis = monitor.timeoutMs.toLong() }
                if (!monitor.headers.isNullOrBlank()) {
                    try {
                        val hm = Json.decodeFromString<Map<String, String>>(monitor.headers)
                        hm.forEach { (k, v) -> header(k, v) }
                    } catch (e: Exception) { }
                }
                if (!monitor.requestBody.isNullOrBlank()) setBody(monitor.requestBody)
            }
            statusCode = response.status.value
            bodyString = response.bodyAsText()
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            errorCause = e.localizedMessage ?: "Connection Failed"
            0L
        }

        if (errorCause == null) {
            if (statusCode != monitor.expectedStatusCode) {
                errorCause = "Expected HTTP ${monitor.expectedStatusCode}, got $statusCode"
            } else if (!monitor.expectedKeyword.isNullOrBlank() && !bodyString.contains(monitor.expectedKeyword)) {
                errorCause = "Keyword '${monitor.expectedKeyword}' not found"
            } else if (sslExpiry != null && sslExpiry!! < 7) {
                errorCause = "SSL expiring in $sslExpiry days"
                isUp = false
            } else {
                isUp = true
            }
        }

        return CheckResult(isUp, statusCode, timeTaken, errorCause, sslExpiry)
    }
}
