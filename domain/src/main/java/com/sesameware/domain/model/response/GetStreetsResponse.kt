package com.sesameware.domain.model.response

import com.google.gson.annotations.SerializedName

typealias GetStreetsResponse = ApiResult<List<StreetsData>?>

data class StreetsData(
    @SerializedName("name")
    val name: String = "", // Коммунальная
    @SerializedName("streetId")
    val streetId: Int = 0, // 1325
    @SerializedName("type")
    val type: String = "" // улица
)
