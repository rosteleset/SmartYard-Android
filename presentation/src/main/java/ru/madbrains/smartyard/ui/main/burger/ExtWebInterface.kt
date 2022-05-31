package ru.madbrains.smartyard.ui.main.burger

import android.webkit.JavascriptInterface
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.smartyard.Event
import timber.log.Timber

class ExtWebInterface(
    private val viewModel: ExtWebViewModel,
    private val callback: Callback? = null
) : KoinComponent {
    private val preferenceStorage: PreferenceStorage by inject()

    @JavascriptInterface
    fun bearerToken(): String {
        return preferenceStorage.authToken ?: ""
    }

    @JavascriptInterface
    fun postLoadingStarted() {
        Timber.d("debug_web Javascript call postLoadingStarted")
        callback?.onPostLoadingStarted()
    }

    @JavascriptInterface
    fun postLoadingFinished() {
        Timber.d("debug_web Javascript call postLoadingFinished")
        callback?.onPostLoadingFinished()
    }

    @JavascriptInterface
    fun postloadingFinished() {
        Timber.d("debug_web Javascript call postloadingFinished")
        callback?.onPostLoadingFinished()
    }

    @JavascriptInterface
    fun postRefreshParent(timeout: Int) {
        Timber.d("debug_web Javascript call postRefreshParent")
        viewModel.onPostRefreshParent.postValue(Event(timeout))
    }

    companion object {
        const val WEB_INTERFACE_OBJECT = "Android"
    }

    interface Callback {
        fun onPostLoadingStarted()
        fun onPostLoadingFinished()
    }
}
