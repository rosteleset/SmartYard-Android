package com.sesameware.smartyard_oem

import ru.ok.tracer.crash.report.TracerCrashReport

object Crashlytics {
    fun getInstance(): Crashlytics {
        return this
    }

    fun setUserId(userId: String) {
        TracerCrashReport.log("userId = $userId")
    }

    fun setCustomKey(key: String, value: String) {
        TracerCrashReport.log("$key = $value")
    }

    fun recordException(e: Throwable) {
        TracerCrashReport.report(e)
    }
}
