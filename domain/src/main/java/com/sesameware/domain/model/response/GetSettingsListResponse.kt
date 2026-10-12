package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias GetSettingsListResponse = ApiResult<List<Settings>>?

data class Settings(
    @param:Json(name = "address")
    val address: String = "",
    @param:Json(name = "clientId")
    val clientId: String = "",
    @param:Json(name = "clientName")
    val clientName: String = "",
    @param:Json(name = "contractName")
    val contractName: String = "",
    @param:Json(name = "contractOwner")
    val _contractOwner: String = "",
    @param:Json(name = "flatId")
    val flatId: Int = -1,
    @param:Json(name = "flatNumber")
    val flatNumber: String = "",
    @param:Json(name = "flatOwner")
    val _flatOwner: String = "",
    @param:Json(name = "hasGates")
    val _hasGates: String = "",
    @param:Json(name = "houseId")
    val houseId: Int = -1,
    @param:Json(name = "lcab")
    val lcab: String?,
    @param:Json(name = "roommates")
    val roommates: List<Roommate> = listOf(),
    @param:Json(name = "services")
    val services: List<String> = listOf(),
    @param:Json(name = "hasPlog")
    val _hasPlog: String = "",

    @param:Json(name = "availableServices") val availableServices: List<ServicesData>? = null
) {
    val hasGates: Boolean
        get() = _hasGates == "t"

    val hasPlog: Boolean
        get() = _hasPlog == "t"

    val flatOwner: Boolean
        get() = _flatOwner == "t" || _contractOwner == "t"

    data class Roommate(
        @param:Json(name = "expire")
        val expire: String = "",
        @param:Json(name = "phone")
        val phone: String = "",
        @param:Json(name = "type")
        val type: String = ""
    )
}
