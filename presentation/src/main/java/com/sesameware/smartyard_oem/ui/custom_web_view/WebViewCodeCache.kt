package com.sesameware.smartyard_oem.ui.custom_web_view

import java.util.UUID

object WebViewCodeCache {
    private const val CACHE_PREFIX = "cache_key_"
    private const val MAX_BUNDLE_STRING_SIZE = 100 * 1024 // 100KB

    private val cache = mutableMapOf<String, String>()

    fun put(code: String?): String? {
        if (code == null) return null
        if (code.length < MAX_BUNDLE_STRING_SIZE) return code

        val key = CACHE_PREFIX + UUID.randomUUID().toString()
        cache[key] = code
        return key
    }

    fun get(key: String?): String? {
        if (key == null) return null
        if (!key.startsWith(CACHE_PREFIX)) return key

        return cache[key]
    }

    // Optional: clear cache to avoid memory leaks if many large strings are opened
    fun clear() {
        cache.clear()
    }
}
