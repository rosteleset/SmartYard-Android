package com.sesameware.data.repository

import com.sesameware.data.prefs.PreferenceStorage
import kotlinx.coroutines.sync.Mutex
import java.security.MessageDigest

/** Shared by repository instances so logout invalidates requests already in flight. */
class OfflineCacheSession {
    val mutex = Mutex()
    var generation: Long = 0
        private set

    fun invalidate() { generation++ }

    fun owner(preferences: PreferenceStorage, baseUrl: String): String? {
        val token = preferences.authToken?.takeIf { it.isNotBlank() } ?: return null
        return MessageDigest.getInstance("SHA-256")
            .digest("$baseUrl\n$token".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
