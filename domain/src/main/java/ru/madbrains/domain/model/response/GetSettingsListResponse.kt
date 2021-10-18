package ru.madbrains.domain.model.response

import com.squareup.moshi.Json

typealias GetSettingsListResponse = ApiResult<List<Settings>>?

data class Settings(
    @Json(name = "address")
    val address: String = "", // Тамбов, ул. Уборевича, дом 7, кв 1
    @Json(name = "clientId")
    val clientId: String = "", // 75549
    @Json(name = "clientName")
    val clientName: String = "", // Пупкин Василий Алибабаевич (6c:3b:6b:4c:73:c5)
    @Json(name = "contractName")
    val contractName: String = "", // ФЛ-70392/18
    @Json(name = "contractOwner")
    val contractOwner: String = "", // t
    @Json(name = "flatId")
    val flatId: Int = -1, // 1
    @Json(name = "flatNumber")
    val flatNumber: String = "",
    @Json(name = "flatOwner")
    val flatOwner: String = "", // t
    @Json(name = "hasGates")
    val hasGates: String = "", // t
    @Json(name = "houseId")
    val houseId: Int = -1, // 6556
    @Json(name = "lcab")
    val lcab: String?, // https://lc.lanta.me/?auth=ZjcwMzkyOmQ2Mjg3ZjdlMDVjYzU2ODM0MmM5ZDUwOGU4ZDg2Njll
    @Json(name = "roommates")
    val roommates: List<Roommate> = listOf(),
    @Json(name = "services")
    val services: List<String> = listOf(),
    @Json(name = "hasPlog")
    val hasPlog: String = ""
) {
    data class Roommate(
        @Json(name = "expire")
        val expire: String = "", // 3001-01-01 00:00:00
        @Json(name = "phone")
        val phone: String = "", // 79876317574
        @Json(name = "type")
        val type: String = "" // inner
    )
}
