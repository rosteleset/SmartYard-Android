package com.sesameware.data.interceptors

import android.os.Build
import com.sesameware.data.BuildConfig
import com.sesameware.data.DataModule
import okhttp3.Interceptor
import okhttp3.Response
import com.sesameware.data.prefs.PreferenceStorage
import java.util.Locale

/**
 * @author Artem Budarin
 * Created on 2019-11-08.
 */
class SessionInterceptor constructor(
    private val preferenceStorage: PreferenceStorage
) : Interceptor {

    companion object {
        private const val KEY_TOKEN = "Authorization"
        private const val VALUE_TOKEN = "Bearer %s"
        private const val X_DM_API_REFRESH = "X-Dm-Api-Refresh"
        private const val USER_AGENT = "User-Agent"
        private const val X_SYSTEM_INFO = "X-System-Info"
        private const val ACCEPT_LANGUAGE = "Accept-Language"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = preferenceStorage.authToken

        val originalRequest = chain.request()
        val customRequest = originalRequest.newBuilder()

        if (!token.isNullOrBlank()) {
            customRequest.addHeader(KEY_TOKEN, String.format(VALUE_TOKEN, token))
        }

        if (BuildConfig.USER_AGENT.isNotBlank()) {
            customRequest.addHeader(USER_AGENT, BuildConfig.USER_AGENT)
        }

        if (preferenceStorage.xDmApiRefresh || DataModule.xDmApiRefresh) {
            customRequest.addHeader(X_DM_API_REFRESH, "")
            preferenceStorage.xDmApiRefresh = false
            DataModule.xDmApiRefresh = false
        }

        // System Information header
        val xSystemInfo = "Android,${Build.VERSION.RELEASE},${Build.MANUFACTURER},${Build.MODEL}"
        customRequest.addHeader(X_SYSTEM_INFO, xSystemInfo)

        // Language header
        customRequest.addHeader(ACCEPT_LANGUAGE, Locale.getDefault().language)

        return chain.proceed(customRequest.build())
    }
}
