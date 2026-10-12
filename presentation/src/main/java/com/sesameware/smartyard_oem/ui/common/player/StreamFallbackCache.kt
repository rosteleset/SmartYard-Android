package com.sesameware.smartyard_oem.ui.common.player

import android.net.Uri
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object StreamFallbackCache {
    private val failedUris: MutableSet<Uri> = Collections.newSetFromMap(ConcurrentHashMap())

    fun isFailed(uri: Uri): Boolean = failedUris.contains(uri)
    fun markFailed(uri: Uri) { failedUris.add(uri) }
    fun clear() { failedUris.clear() }
}