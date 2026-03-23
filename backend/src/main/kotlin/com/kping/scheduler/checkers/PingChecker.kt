package com.kping.scheduler.checkers

import com.kping.models.MonitorDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.URL

class PingChecker : MonitorChecker {
    override suspend fun check(monitor: MonitorDto): CheckResult {
        return withContext(Dispatchers.IO) {
            var timeTaken = 0L
            var isUp = false
            var errorCause: String? = null
            
            try {
                val host = try { 
                    URL(monitor.url).host.takeIf { it.isNotEmpty() } ?: monitor.url 
                } catch(e: Exception) { monitor.url }
                
                val address = InetAddress.getByName(host)
                val start = System.currentTimeMillis()
                isUp = address.isReachable(monitor.timeoutMs)
                timeTaken = System.currentTimeMillis() - start
                
                if (!isUp) errorCause = "ICMP Ping timeout"
            } catch (e: Exception) {
                errorCause = e.localizedMessage ?: "Unknown host or network error"
            }
            
            CheckResult(isUp, if(isUp) 200 else 0, timeTaken, errorCause)
        }
    }
}
