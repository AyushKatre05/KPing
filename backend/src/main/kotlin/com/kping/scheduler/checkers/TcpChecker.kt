package com.kping.scheduler.checkers

import com.kping.models.MonitorDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class TcpChecker : MonitorChecker {
    override suspend fun check(monitor: MonitorDto): CheckResult {
        return withContext(Dispatchers.IO) {
            var isUp = false
            var timeTaken = 0L
            var errorCause: String? = null
            
            val port = monitor.port ?: 80
            val host = try { 
                URL(monitor.url).host.takeIf { it.isNotEmpty() } ?: monitor.url 
            } catch(e: Exception) { monitor.url }

            try {
                val start = System.currentTimeMillis()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), monitor.timeoutMs)
                    isUp = true
                }
                timeTaken = System.currentTimeMillis() - start
            } catch (e: Exception) {
                errorCause = e.localizedMessage ?: "TCP Connection Refused"
            }

            CheckResult(isUp, if(isUp) 200 else 0, timeTaken, errorCause)
        }
    }
}
