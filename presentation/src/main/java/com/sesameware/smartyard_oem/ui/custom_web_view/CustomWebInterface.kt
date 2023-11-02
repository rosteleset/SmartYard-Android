package com.sesameware.smartyard_oem.ui.custom_web_view

import android.webkit.JavascriptInterface
import com.sesameware.data.prefs.PreferenceStorage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CustomWebInterface(private val callback: Callback? = null) : KoinComponent {
    private val preferenceStorage: PreferenceStorage by inject()

    @JavascriptInterface
    fun bearerToken(): String {
        return preferenceStorage.authToken ?: ""
    }

    @JavascriptInterface
    fun postLoadingStarted() {
        callback?.onPostLoadingStarted()
    }

    @JavascriptInterface
    fun postLoadingFinished() {
        callback?.onPostLoadingFinished()
    }

    @JavascriptInterface
    fun postloadingFinished() {
        callback?.onPostLoadingFinished()
    }

    @JavascriptInterface
    fun postRefreshParent(timeout: Int) {
        callback?.onPostRefreshParent(timeout)
    }

    interface Callback {
        fun onPostLoadingStarted()
        fun onPostLoadingFinished()
        fun onPostRefreshParent(timeout: Int)
    }

    companion object {
        const val WEB_INTERFACE_OBJECT = "Android"
    }
}
