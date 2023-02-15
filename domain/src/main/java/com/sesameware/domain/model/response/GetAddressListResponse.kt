package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 06/03/2020.
 */
typealias GetAddressListResponse = ApiResult<List<Address>>?

data class Address(
    @Json(name = "address")
    val address: String = "", // Тамбов, ул. Уборевича, дом 9
    @Json(name = "cctv")
    val cctv: Int = 0, // 1
    @Json(name = "doors")
    val doors: List<Door> = listOf(),
    @Json(name = "houseId")
    val houseId: Int,
    @Json(name = "hasPlog")
    val _hasPlog: String = ""
) {
    val hasPlog: Boolean
        get() = _hasPlog == "t"

    data class Door(
        @Json(name = "domophoneId")
        val domophoneId: Int = 0, // 53
        @Json(name = "doorId")
        val doorId: Int = 0, // 0
        @Json(name = "entrance")
        val entrance: String = "", // 1
        @Json(name = "icon")
        val icon: String = "", // barrier
        @Json(name = "name")
        val name: String = "" // Шлагбаум
    )
}
