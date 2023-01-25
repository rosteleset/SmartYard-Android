package com.sesameware.data.interceptors

import com.sesameware.data.BuildConfig
import com.sesameware.data.DataModule
import okhttp3.Interceptor
import okhttp3.Response
import com.sesameware.data.prefs.PreferenceStorage

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
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = preferenceStorage.authToken

        val originalRequest = chain.request()
        val tokenRequest = originalRequest.newBuilder()

        if (!token.isNullOrBlank()) {
            tokenRequest.addHeader(KEY_TOKEN, String.format(VALUE_TOKEN, token))
        }

        if (BuildConfig.USER_AGENT.isNotBlank()) {
            tokenRequest.addHeader(USER_AGENT, BuildConfig.USER_AGENT)
        }

        if (preferenceStorage.xDmApiRefresh || DataModule.xDmApiRefresh) {
            tokenRequest.addHeader(X_DM_API_REFRESH, "")
            preferenceStorage.xDmApiRefresh = false
            DataModule.xDmApiRefresh = false
        }

        return chain.proceed(tokenRequest.build())
    }
}
