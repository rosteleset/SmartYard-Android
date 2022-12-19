package com.sesameware.data.interceptors

import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author Artem Budarin
 * Created on 2019-11-08.
 */
class CommonInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request)
    }
}
