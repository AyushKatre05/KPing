package com.kping.scheduler

import com.kping.models.MonitorLogs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object DataGroomerWorker {
    private const val RETENTION_DAYS = 30L
    private const val RUN_INTERVAL_MS = 24L * 60L * 60L * 1000L // 24 hours

    suspend fun start() {
        withContext(Dispatchers.IO) {
            // Give the system a brief moment to boot before running a massive delete query
            delay(10000)
            while (isActive) {
                try {
                    val cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS)
                    val deletedCount = transaction {
                        MonitorLogs.deleteWhere { MonitorLogs.checkedAt less cutoffDate }
                    }
                    if (deletedCount > 0) {
                        println("[DataGroomer] Successfully purged $deletedCount expired monitor logs older than $RETENTION_DAYS days.")
                    }
                } catch (e: Exception) {
                    println("[DataGroomer] Error during data grooming sequence: ${e.message}")
                }
                
                delay(RUN_INTERVAL_MS)
            }
        }
    }
}
