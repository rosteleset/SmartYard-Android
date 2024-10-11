package ru.madbrains.domain.model.response

import android.os.Parcelable
import androidx.compose.runtime.Stable
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter


typealias CCTVGetResponse = ApiResult<List<CCTVData>>
typealias CCTVCityCameraGetResponse = ApiResult<List<CCTVCityCameraData>>
typealias CCTVYoutubeResponse = ApiResult<List<CCTVYoutubeData>>

private val mPreviewFormatter = DateTimeFormatter.ofPattern("YYYY/MM/dd/HH/mm/ss")
val targetZoneId: ZoneId = ZoneId.of("GMT+7")

@Parcelize
data class Door(
    val domophoneId: Long,
    val doorId: Int,
    val icon: String,
    val name: String
) : Parcelable

@Stable
@Parcelize
data class CCTVData(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "lat") val latitude: Double,
    @Json(name = "lon") val longitude: Double,
    @Json(name = "token") val token: String,
    @Json(name = "url") val url: String,
    @Json(name = "doors") val doors: List<Door>?,
    @Json(name = "flatIds") val flatIds: List<String>?,
) : Parcelable {
//        val hls: String get() = "$url/index.m3u8?token=$token" //Старый вариант
    val hls: String get() = "$url/mpegts?token=$token" //Наш вариант
    val preview: String get() = "$url/preview.mp4?token=$token"
    val imageCamera: String get() = "https://intercom-mobile-api.mycentra.ru/event/get/url/$id"

    fun getHlsAt(time: LocalDateTime, durationSeconds: Long): String {
        val zoned = time.atZone(targetZoneId).withZoneSameInstant(ZoneId.systemDefault())
        val timeStamp = DateTimeUtils.toSqlTimestamp(zoned.toLocalDateTime()).time / 1000
        return "$url/index-$timeStamp-$durationSeconds.m3u8?token=$token"
    }

    fun getPreviewAt(time: LocalDateTime): String {
        val zoned = time.atZone(targetZoneId).withZoneSameInstant(ZoneId.of("UTC"))
        return "$url/${zoned.format(mPreviewFormatter)}-preview.mp4?token=$token"
    }
}

@Parcelize
data class CCTVCityCameraData(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "lat") val latitude: Double?,
    @Json(name = "lon") val longitude: Double?,
    @Json(name = "url") val url: String,
    @Json(name = "token") val token: String,
    @Json(name = "fullUrl") val fullUrl: String,
    @Json(name = "screenshotUrl") val screenshotUrl: String,
) : Parcelable {
//        val hls: String get() = "$url/index.m3u8?token=$token"
    val hls: String get() = "$url/mpegts?token=$token" //Наш вариант
    val preview: String get() = "$url/preview.mp4?token=$token"
    fun screenShotPreview(mainUrl: String) = "$mainUrl$screenshotUrl"
}

@Parcelize
data class CCTVYoutubeData(
    @Json(name = "id") val id: Int,
    @Json(name = "eventTime") val eventTime: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "thumbnailsDefault") val thumbnailsDefault: String,
    @Json(name = "thumbnailsMedium") val thumbnailsMedium: String,
    @Json(name = "thumbnailsHigh") val thumbnailsHigh: String,
    @Json(name = "url") val url: String
) : Parcelable

