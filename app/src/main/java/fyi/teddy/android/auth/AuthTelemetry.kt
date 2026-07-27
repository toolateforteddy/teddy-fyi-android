package fyi.teddy.android.auth

import android.util.Log
import java.util.ArrayDeque

object AuthTelemetry {
    private const val BUFFER_CAPACITY = 30

    data class Breadcrumb(
        val timestamp: Long = System.currentTimeMillis(),
        @Suppress("NewApi")
        val threadId: Long = Thread.currentThread().threadId(),
        val threadName: String = Thread.currentThread().name,
        val action: String,
        val details: String
    ) {
        fun format(): String =
            "[$timestamp] [Thread-$threadId:$threadName] $action: $details"
    }

    private val buffer = ArrayDeque<Breadcrumb>(BUFFER_CAPACITY)

    @Synchronized
    fun logBreadcrumb(action: String, details: String) {
        if (buffer.size >= BUFFER_CAPACITY) {
            buffer.pollFirst()
        }
        val breadcrumb = Breadcrumb(action = action, details = details)
        buffer.addLast(breadcrumb)
        Log.d("AuthTelemetry", breadcrumb.format())
    }

    @Synchronized
    fun getBreadcrumbs(): List<String> {
        return buffer.map { it.format() }
    }

    @Synchronized
    fun clear() {
        buffer.clear()
    }

    @Synchronized
    fun flushBreadcrumbs(reason: String) {
        val triggerMessage = "LOGOUT_TRIGGERED: $reason"
        Log.e("AuthTelemetry", "================ AUTH TELEMETRY FLUSH ================")
        Log.e("AuthTelemetry", triggerMessage)
        for (breadcrumb in buffer) {
            Log.e("AuthTelemetry", "  " + breadcrumb.format())
        }
        Log.e("AuthTelemetry", "======================================================")
    }

    fun maskToken(token: String?): String {
        if (token == null) return "null"
        if (token.isBlank()) return "empty"
        return if (token.length <= 10) {
            "***"
        } else {
            "${token.take(6)}...${token.takeLast(4)}"
        }
    }
}
