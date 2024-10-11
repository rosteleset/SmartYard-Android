package ru.madbrains.domain.model.response

import com.squareup.moshi.Json


typealias CameraCctvResponse = ApiResult<List<CameraCctvItemItem>>?


data class CameraCctvItemItem(
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

    @Json(name = "url")
    val url: String,

    @Json(name = "fullUrl")
    val fullUrl: String,

    @Json(name = "screenshotUrl")
    val screenshotUrl: String,

    val isCityCctv: Boolean = false
){
    fun preview() = "${url}/preview.mp4?token=${token}"
    val redisPreview = "https://intercom-mobile-api.mycentra.ru/event/get/url/$id"
    fun screenShotPreview(mainUrl: String) = "$mainUrl$screenshotUrl"
}