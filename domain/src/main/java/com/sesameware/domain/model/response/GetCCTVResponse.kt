package com.sesameware.domain.model.response

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

typealias CCTVGetResponse = ApiResult<List<CCTVData>>
typealias CCTVCityCameraGetResponse = ApiResult<List<CCTVCityCameraData>>
typealias CCTVYoutubeResponse = ApiResult<List<CCTVYoutubeData>>
typealias CCTVRangesResponse = ApiResult<List<RangeObject>>
private val mPreviewFormatter = DateTimeFormatter.ofPattern("YYYY/MM/dd/HH/mm/ss")
private val mPreviewFormatterMacroscop = DateTimeFormatter.ofPattern("dd.MM.yyyy%20HH:mm:ss")

@Parcelize
data class CCTVData(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "lat") val latitude: Double?,
    @Json(name = "lon") val longitude: Double?,
    @Json(name = "token") val token: String,
    @Json(name = "url") val url: String,
    @Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC,
) : Parcelable {
    val hls: String get() =
        when (serverType) {
            MediaServerType.NIMBLE -> "$url/playlist.m3u8?wmsAuthSign=$token"
            MediaServerType.MACROSCOP -> "$url&$token"
            MediaServerType.FORPOST -> "$url&$token"
            else -> "$url/index.m3u8?token=$token"
        }

    val preview: String get() =
        when (serverType) {
            MediaServerType.NIMBLE -> "$url/thumbnail.mp4?wmsAuthSign=$token"
            MediaServerType.FORPOST -> "$url&$token"
            else -> "$url/preview.mp4?token=$token"
        }

    fun getHlsAt(time: LocalDateTime, durationSeconds: Long, timeZone: String): String {
        val zoned = time.atZone(ZoneId.of(timeZone))
        val timeStamp = DateTimeUtils.toSqlTimestamp(zoned.toLocalDateTime()).time / 1000
        return when (serverType) {
            MediaServerType.NIMBLE -> "$url/playlist_dvr_range-$timeStamp-$durationSeconds.m3u8?wmsAuthSign=$token"
            MediaServerType.MACROSCOP -> "$url&$token"
            MediaServerType.FORPOST -> "$url&$token"
            else -> "$url/index-$timeStamp-$durationSeconds.m3u8?token=$token"
        }
    }

    fun getPreviewAt(time: LocalDateTime, timeZone: String): String {
        val zoned = time.atZone(ZoneId.of(timeZone)).withZoneSameInstant(ZoneId.of("UTC"))
        val ts = zoned.toEpochSecond()
        val tz = Instant.now().atZone(ZoneId.of(timeZone)).offset.totalSeconds
        return when (serverType) {
            MediaServerType.NIMBLE -> "$url/dvr_thumbnail_${zoned.format(mPreviewFormatter)}.mp4?wmsAuthSign=$token"
            MediaServerType.MACROSCOP -> "${url.replace("/hls?", "/site?")}&$token&starttime=${zoned.format(mPreviewFormatterMacroscop)}&resolutionx=480&resolutiony=270&streamtype=mainvideo&withcontenttype=true&mode=archive"
            MediaServerType.FORPOST -> "$url&$token&TS=$ts&TZ=$tz"
            else -> "$url/${zoned.format(mPreviewFormatter)}-preview.mp4?token=$token"
        }
    }

    val serverType: MediaServerType
        get() {
            return when (_serverType) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                MediaServerType.MEDIA_TYPE_MACROSCOP -> MediaServerType.MACROSCOP
                MediaServerType.MEDIA_TYPE_FORPOST -> MediaServerType.FORPOST
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
    @Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC,
) : Parcelable {
    val hls: String get() = when (serverType) {
        MediaServerType.NIMBLE -> "$url/playlist.m3u8?wmsAuthSign=$token"
        MediaServerType.MACROSCOP -> "$url&$token"
        MediaServerType.FORPOST -> "$url&$token"
        else -> "$url/index.m3u8?token=$token"
    }

    val serverType: MediaServerType
        get() {
            return when (_serverType) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                MediaServerType.MEDIA_TYPE_MACROSCOP -> MediaServerType.MACROSCOP
                MediaServerType.MEDIA_TYPE_FORPOST -> MediaServerType.FORPOST
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
    NIMBLE,
    MACROSCOP,
    FORPOST;

    companion object {
        const val MEDIA_TYPE_FLUSSONIC = "flussonic"
        const val MEDIA_TYPE_NIMBLE = "nimble"
        const val MEDIA_TYPE_MACROSCOP = "macroscop"
        const val MEDIA_TYPE_FORPOST = "forpost"
    }
}
