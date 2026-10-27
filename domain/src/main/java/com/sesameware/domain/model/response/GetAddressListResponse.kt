package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 06/03/2020.
 */
typealias GetAddressListResponse = ApiResult<List<Address>>?

data class Address(
    @param:Json(name = "address")
    val address: String = "", // Тамбов, ул. Уборевича, дом 9
    @param:Json(name = "cctv")
    val cctv: Int = 0, // 1
    @param:Json(name = "doors")
    val doors: List<Door> = listOf(),
    @param:Json(name = "houseId")
    val houseId: Int,
    @param:Json(name = "hasPlog")
    val _hasPlog: String = "",
    @param:Json(name = "ext")
    val ext: List<ItemExt> = listOf()
) {
    val hasPlog: Boolean
        get() = _hasPlog == "t"

    data class Door(
        @param:Json(name = "domophoneId")
        val domophoneId: Int = 0, // 53
        @param:Json(name = "doorId")
        val doorId: Int = 0, // 0
        @param:Json(name = "entrance")
        val entrance: String = "", // 1
        @param:Json(name = "icon")
        val icon: String = "", // barrier
        @param:Json(name = "name")
        val name: String = "", // Шлагбаум
        @param:Json(name = "doorCode")
        val doorCode: String? = null
    )
}
