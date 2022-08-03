package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias ExtListResponse = ApiResult<List<ItemExt>>?

data class ItemExt(
    @Json(name = "caption") val caption: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "order") val order: Int? = null,
    @Json(name = "extId") val extId: String? = null,
    @Json(name = "highlight") val _highlight: String = "f"
) {
    val highlight: Boolean
        get() = _highlight == "t"
}
