package ru.madbrains.domain.model.response

import android.os.Parcelable
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
val targetZoneId: ZoneId = ZoneId.of("GMT+3")

@Parcelize
data class CCTVData(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "lat") val latitude: Double,
    @Json(name = "lon") val longitude: Double,
    @Json(name = "token") val token: String,
    @Json(name = "url") val url: String
) : Parcelable {
    val hls: String get() = "$url/index.m3u8?token=$token"
    val preview: String get() = "$url/preview.mp4?token=$token"
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
    @Json(name = "token") val token: String
) : Parcelable {
    val hls: String get() = "$url/index.m3u8?token=$token"
    val preview: String get() = "$url/preview.mp4?token=$token"
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
