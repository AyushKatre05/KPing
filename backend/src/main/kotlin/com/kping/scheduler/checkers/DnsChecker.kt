package com.kping.scheduler.checkers

import com.kping.models.MonitorDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class DnsChecker : MonitorChecker {
    override suspend fun check(monitor: MonitorDto): CheckResult {
        return withContext(Dispatchers.IO) {
            var isUp = false
            var timeTaken = 0L
            var errorCause: String? = null
            
            // Expected Keyword holds the expected IP address
            try {
                val start = System.currentTimeMillis()
                val addresses = InetAddress.getAllByName(monitor.url)
                timeTaken = System.currentTimeMillis() - start
                
                if (addresses.isEmpty()) {
                    errorCause = "No DNS records found"
                } else if (!monitor.expectedKeyword.isNullOrBlank()) {
                    val ips = addresses.map { it.hostAddress }
                    if (monitor.expectedKeyword !in ips) {
                        errorCause = "Expected IP '${monitor.expectedKeyword}' not in resolved list: $ips"
                    } else {
                        isUp = true
                    }
                } else {
                    isUp = true
                }
            } catch (e: Exception) {
                errorCause = e.localizedMessage ?: "DNS Resolution Failed"
            }

            CheckResult(isUp, if (isUp) 200 else 0, timeTaken, errorCause)
        }
    }
}
