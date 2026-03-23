package com.kping.scheduler.checkers

import com.kping.models.MonitorDto

data class CheckResult(
    val isUp: Boolean,
    val statusCode: Int,
    val responseTimeMs: Long,
    val errorCause: String?,
    val sslExpiryDays: Int? = null
)

interface MonitorChecker {
    suspend fun check(monitor: MonitorDto): CheckResult
}
