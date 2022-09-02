package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias GetSettingsListResponse = ApiResult<List<Settings>>?

data class Settings(
    @Json(name = "address")
    val address: String = "",
    @Json(name = "clientId")
    val clientId: String = "",
    @Json(name = "clientName")
    val clientName: String = "",
    @Json(name = "contractName")
    val contractName: String = "",
    @Json(name = "contractOwner")
    val _contractOwner: String = "",
    @Json(name = "flatId")
    val flatId: Int = -1,
    @Json(name = "flatNumber")
    val flatNumber: String = "",
    @Json(name = "flatOwner")
    val _flatOwner: String = "",
    @Json(name = "hasGates")
    val _hasGates: String = "",
    @Json(name = "houseId")
    val houseId: Int = -1,
    @Json(name = "lcab")
    val lcab: String?,
    @Json(name = "roommates")
    val roommates: List<Roommate> = listOf(),
    @Json(name = "services")
    val services: List<String> = listOf(),
    @Json(name = "hasPlog")
    val _hasPlog: String = ""
) {
    val hasGates: Boolean
        get() = _hasGates == "t"

    val hasPlog: Boolean
        get() = _hasPlog == "t"

    val flatOwner: Boolean
        get() = _flatOwner == "t" || _contractOwner == "t"

    data class Roommate(
        @Json(name = "expire")
        val expire: String = "",
        @Json(name = "phone")
        val phone: String = "",
        @Json(name = "type")
        val type: String = ""
    )
}
