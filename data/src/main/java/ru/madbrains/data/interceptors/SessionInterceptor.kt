package ru.madbrains.data.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import ru.madbrains.data.prefs.PreferenceStorage

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
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = preferenceStorage.authToken

        val originalRequest = chain.request()
        val tokenRequest = originalRequest.newBuilder()

        if (!token.isNullOrBlank()) {
            tokenRequest.addHeader(KEY_TOKEN, String.format(VALUE_TOKEN, token))
        }

        if (preferenceStorage.xDmApiRefresh) {
            tokenRequest.addHeader(X_DM_API_REFRESH, "")
            preferenceStorage.xDmApiRefresh = false
        }

        return chain.proceed(tokenRequest.build())
    }
}
