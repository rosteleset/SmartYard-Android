package com.sesameware.domain.model.response

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
typealias CCTVRangesResponse = ApiResult<List<RangeObject>>
private val mPreviewFormatter = DateTimeFormatter.ofPattern("YYYY/MM/dd/HH/mm/ss")
val targetZoneId: ZoneId = ZoneId.of("GMT+3")

@Parcelize
data class CCTVData(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "lat") val latitude: Double?,
    @Json(name = "lon") val longitude: Double?,
    @Json(name = "token") val token: String,
    @Json(name = "url") val url: String,
    @Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC
) : Parcelable {
    val hls: String get() =
        when (serverType) {
            MediaServerType.NIMBLE -> "$url/playlist.m3u8?wmsAuthSign=$token"
            else -> "$url/index.m3u8?token=$token"
        }

    val preview: String get() =
        when (serverType) {
            MediaServerType.NIMBLE -> "$url/thumbnail.mp4?wmsAuthSign=$token"
            else -> "$url/preview.mp4?token=$token"
        }

    fun getHlsAt(time: LocalDateTime, durationSeconds: Long): String {
        val zoned = time.atZone(targetZoneId).withZoneSameInstant(ZoneId.systemDefault())
        val timeStamp = DateTimeUtils.toSqlTimestamp(zoned.toLocalDateTime()).time / 1000
        return when (serverType) {
            MediaServerType.NIMBLE -> "$url/playlist_dvr_range-$timeStamp-$durationSeconds.m3u8?wmsAuthSign=$token"
            else -> "$url/index-$timeStamp-$durationSeconds.m3u8?token=$token"
        }
    }

    fun getPreviewAt(time: LocalDateTime): String {
        val zoned = time.atZone(targetZoneId).withZoneSameInstant(ZoneId.of("UTC"))
        return when (serverType) {
            MediaServerType.NIMBLE -> "$url/dvr_thumbnail_${zoned.format(mPreviewFormatter)}.mp4?wmsAuthSign=$token"
            else -> "$url/${zoned.format(mPreviewFormatter)}-preview.mp4?token=$token"
        }
    }

    val serverType: MediaServerType
        get() {
            return when (_serverType) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                else -> MediaServerType.FLUSSONIC
            }
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
    @Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC
) : Parcelable {
    val hls: String get() = when (serverType) {
        MediaServerType.NIMBLE -> "$url/playlist.m3u8?wmsAuthSign=$token"
        else -> "$url/index.m3u8?token=$token"
    }

    val serverType: MediaServerType
        get() {
            return when (_serverType) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                else -> MediaServerType.FLUSSONIC
            }
        }
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

enum class MediaServerType {
    FLUSSONIC,
    NIMBLE;

    companion object {
        const val MEDIA_TYPE_FLUSSONIC = "flussonic"
        const val MEDIA_TYPE_NIMBLE = "nimble"
    }
}