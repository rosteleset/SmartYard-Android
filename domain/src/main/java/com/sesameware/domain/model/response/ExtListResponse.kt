package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias ExtListResponse = ApiResult<List<ItemExt>>?

data class ItemExt(
    @param:Json(name = "caption") val caption: String? = null,
    @param:Json(name = "icon") val icon: String? = null,
    @param:Json(name = "order") val order: Int? = null,
    @param:Json(name = "extId") val extId: String? = null,
    @param:Json(name = "highlight") val _highlight: String = "f"
) {
    val highlight: Boolean
        get() = _highlight == "t"
}
