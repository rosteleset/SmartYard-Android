package ru.madbrains.domain.model.response

import com.google.gson.annotations.Expose

import com.squareup.moshi.Json


typealias PlacesCctvResponse = ApiResult<List<PlaceItemItem>>?


data class PlaceItemItem(
    @Json(name = "address")
    val address: String,

    @Json(name = "cctv")
    val cctv: List<Cctv>,

    @Json(name = "clientId")
    val clientId: Int,

    @Json(name = "contractOwner")
    val contractOwner: String,

    @Json(name = "domophoneId")
    val domophoneId: String,

    @Json(name = "doorCode")
    val doorCode: String,

    @Json(name = "doorId")
    val doorId: Int,

    @Json(name = "flatId")
    val flatId: Int,

    @Json(name = "flatNumber")
    val flatNumber: Int,

    @Json(name = "frsEnabled")
    val frsEnabled: String,

    @Json(name = "hasPlog")
    val hasPlog: String,

    @Json(name = "houseId")
    val houseId: Int,

    @Json(name = "icon")
    val icon: String,

    @Json(name = "name")
    val name: String
)


data class Cctv(
    @Json(name = "houseId")
    val houseId: Int,

    @Json(name = "id")
    val id: Int,

    @Json(name = "lat")
    val lat: Double,

    @Json(name = "lon")
    val lon: Double,

    @Json(name = "name")
    val name: String,

    @Json(name = "token")
    val token: String,

    @Json(name = "type")
    val type: String,

    @Json(name = "url")
    val url: String,

    @Json(name = "fullUrl")
    val fullUrl: String,

    @Json(name = "screenshotUrl")
    val screenshotUrl: String,
) {
    val videoUrl = "${url}/index.m3u8?token=${token}"
    val previewUrl = "${url}/preview.mp4?token=${token}"
    val redisPreview = "https://intercom-mobile-api.mycentra.ru/event/get/url/$id"
    fun screenShotPreview(mainUrl: String) = "$mainUrl$screenshotUrl"
}
