package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ExtResponse = ApiResult<Ext>?

data class Ext(
    @Json(name = "basePath") val basePath: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "version") val _version: Int? = null,
    @Json(name = "webViewOptions") val options: WebViewOptions? = null
) {
    val version get() = _version ?: 1
}

data class WebViewOptions(
    @Json(name = "navBarHidden") val _isHeaderHidden: Any? = null,
    @Json(name = "statusBarColor") val statusBarColor: String? = null,
    @Json(name = "statusBarStyle") val statusBarStyle: String? = null,
    @Json(name = "pullToRefreshEnabled") val _pullToRefreshEnabled: Any? = null,
) {
    val isHeaderHidden get() = when (val value = _isHeaderHidden) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("t", ignoreCase = true)
            || value.equals("true", ignoreCase = true)
            || value == "1"
        else -> false
    }

    val pullToRefreshEnabled get() = when (val value = _pullToRefreshEnabled) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> !(value.equals("f", ignoreCase = true)
            || value.equals("false", ignoreCase = true)
            || value == "0")
        else -> true
    }
}
