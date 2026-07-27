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
    @Json(name = "navBarHidden") val _isHeaderHidden: String? = null,
    @Json(name = "statusBarColor") val statusBarColor: String? = null,
    @Json(name = "statusBarStyle") val statusBarStyle: String? = null,
) {
    val isHeaderHidden get() = _isHeaderHidden == "t"
}
