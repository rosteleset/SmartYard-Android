package ru.madbrains.domain.model.response

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
    val _hasPlog: String,
    @Json(name = "intercoms")
    val intercoms: List<Intercoms>
) {
    val hasPlog: Boolean
        get() = _hasPlog == "t"

    data class Intercoms(
        @Json(name = "doors")
        val doors: List<Doors>,
        @Json(name = "code")
        val code: Code,
        @Json(name = "flats")
        val flats: List<Flats>,
        @Json(name = "events")
        val events: Int,
        @Json(name = "status")
        val status: Boolean?,
        @Json(name = "id")
        val id: Int,
        @Json(name = "name")
        val name: String,
        @Json(name = "lat")
        val lat: Double,
        @Json(name = "lon")
        val lon: Double,
        @Json(name = "url")
        val url: String,
        @Json(name = "token")
        val token: String,
        @Json(name = "type")
        val type: String
    )

    data class Code(
        @Json(name = "domophoneId")
        val domophoneId: String,
        @Json(name = "flatId")
        val flatId: Int,
        @Json(name = "doorCode")
        val doorCode: String
    )

    data class Flats(
        @Json(name = "flatId")
        val flatId: Int,
        @Json(name = "flatNumber")
        val flatNumber: Int,
        @Json(name = "frsEnabled")
        val frsEnabled: String,
        @Json(name = "contractOwner")
        val contractOwner: String,
        @Json(name = "hasGates")
        val hasGates: String,
        @Json(name = "clientId")
        val clientId: Int,
    )
    data class Doors(
        @Json(name = "doorId")
        val doorId: Int = 0,
        @Json(name = "icon")
        val icon: String = "",
        @Json(name = "name")
        val name: String = "",
        @Json(name = "domophoneId")
        val domophoneId: Long = 0L,
        @Json(name = "title")
        val title: String = "",
    )

    data class Door(
        @Json(name = "doorId")
        val doorId: Int = 0, // 0
        @Json(name = "icon")
        val icon: String = "", // barrier
        @Json(name = "name")
        val name: String = "", // Шлагбаум
        @Json(name = "domophoneId")
        val domophoneId: Long = 0L, // 53
        @Json(name = "entrance")
        val entrance: String = "", // 1
    )
}
