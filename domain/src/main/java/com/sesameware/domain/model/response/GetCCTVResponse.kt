package com.sesameware.domain.model.response

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

typealias CCTVGetResponse = ApiResult<List<CCTVData>>
typealias CCTVTreeResponse = ApiResult<CCTVDataTree>
typealias CCTVCityCameraGetResponse = ApiResult<List<CCTVCityCameraData>>
typealias CCTVYoutubeResponse = ApiResult<List<CCTVYoutubeData>>
typealias CCTVRangesResponse = ApiResult<List<RangeObject>>
private val mPreviewFormatter = DateTimeFormatter.ofPattern("YYYY/MM/dd/HH/mm/ss")
private val mPreviewFormatterMacroscop = DateTimeFormatter.ofPattern("dd.MM.yyyy%20HH:mm:ss")

@Parcelize
data class CCTVData(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "lat") val latitude: Double?,
    @param:Json(name = "lon") val longitude: Double?,
    @param:Json(name = "token") val token: String,
    @param:Json(name = "url") val url: String,
    @param:Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC,
    @param:Json(name = "hasSound") val _hasSound: Boolean?
) : Parcelable {

    @IgnoredOnParcel
    private val timestamp: Long = System.currentTimeMillis()

    val previewCacheKey: String
        get() = "$preview$timestamp"

    val hls: String get() =
        when (serverType) {
            MediaServerType.NIMBLE -> "$url/playlist.m3u8?wmsAuthSign=$token"
            MediaServerType.MACROSCOP -> "$url&$token"
            MediaServerType.FORPOST -> "$url&$token"
            MediaServerType.SESAMEWARE,
            MediaServerType.FLUSSONIC -> "$url/index.m3u8?token=$token"
        }

    val llhls: String get() =
        when (serverType) {
            MediaServerType.NIMBLE -> "$url/playlist.m3u8?wmsAuthSign=$token"
            MediaServerType.MACROSCOP -> "$url&$token"
            MediaServerType.FORPOST -> "$url&$token"
            MediaServerType.SESAMEWARE,
            MediaServerType.FLUSSONIC -> "$url/llhls.m3u8?token=$token"
        }

    val preview: String get() =
        when (serverType) {
            MediaServerType.NIMBLE -> "$url/thumbnail.mp4?wmsAuthSign=$token"
            MediaServerType.MACROSCOP,
            MediaServerType.FORPOST -> "$url&$token"
            MediaServerType.SESAMEWARE,
            MediaServerType.FLUSSONIC -> "$url/preview.mp4?token=$token"
        }

    fun getHlsAt(time: LocalDateTime, durationSeconds: Long, timeZone: String): String {
        val zoned = time.atZone(ZoneId.of(timeZone))
        val timeStamp = zoned.toEpochSecond()
        return when (serverType) {
            MediaServerType.NIMBLE -> "$url/playlist_dvr_range-$timeStamp-$durationSeconds.m3u8?wmsAuthSign=$token"
            MediaServerType.MACROSCOP -> "$url&$token"
            MediaServerType.FORPOST -> "$url&$token"
            MediaServerType.SESAMEWARE,
            MediaServerType.FLUSSONIC -> "$url/index-$timeStamp-$durationSeconds.m3u8?token=$token"
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
            MediaServerType.SESAMEWARE,
            MediaServerType.FLUSSONIC -> "$url/${zoned.format(mPreviewFormatter)}-preview.mp4?token=$token"
        }
    }

    val serverType: MediaServerType
        get() {
            return when (_serverType) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                MediaServerType.MEDIA_TYPE_MACROSCOP -> MediaServerType.MACROSCOP
                MediaServerType.MEDIA_TYPE_FORPOST -> MediaServerType.FORPOST
                MediaServerType.MEDIA_TYPE_SESAMEWARE -> MediaServerType.SESAMEWARE
                else -> MediaServerType.FLUSSONIC
            }
        }

    val hasSound: Boolean
        get() = _hasSound ?: true
}

@Parcelize
data class CCTVCityCameraData(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "lat") val latitude: Double?,
    @param:Json(name = "lon") val longitude: Double?,
    @param:Json(name = "url") val url: String,
    @param:Json(name = "token") val token: String,
    @param:Json(name = "serverType") val _serverType: String? = MediaServerType.MEDIA_TYPE_FLUSSONIC,
    @param:Json(name = "hasSound") val _hasSound: Boolean?
) : Parcelable {
    val hls: String get() = when (serverType) {
        MediaServerType.NIMBLE -> "$url/playlist.m3u8?wmsAuthSign=$token"
        MediaServerType.MACROSCOP -> "$url&$token"
        MediaServerType.FORPOST -> "$url&$token"
        MediaServerType.SESAMEWARE,
        MediaServerType.FLUSSONIC -> "$url/index.m3u8?token=$token"
    }

    val llhls: String get() = when (serverType) {
        MediaServerType.NIMBLE -> "$url/playlist.m3u8?wmsAuthSign=$token"
        MediaServerType.MACROSCOP -> "$url&$token"
        MediaServerType.FORPOST -> "$url&$token"
        MediaServerType.SESAMEWARE,
        MediaServerType.FLUSSONIC -> "$url/llhls.m3u8?token=$token"
    }

    val serverType: MediaServerType
        get() {
            return when (_serverType) {
                MediaServerType.MEDIA_TYPE_NIMBLE -> MediaServerType.NIMBLE
                MediaServerType.MEDIA_TYPE_MACROSCOP -> MediaServerType.MACROSCOP
                MediaServerType.MEDIA_TYPE_FORPOST -> MediaServerType.FORPOST
                MediaServerType.MEDIA_TYPE_SESAMEWARE -> MediaServerType.SESAMEWARE
                else -> MediaServerType.FLUSSONIC
            }
        }

    val hasSound: Boolean
        get() = _hasSound ?: true
}

@Parcelize
data class CCTVYoutubeData(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "eventTime") val eventTime: String,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "description") val description: String,
    @param:Json(name = "thumbnailsDefault") val thumbnailsDefault: String,
    @param:Json(name = "thumbnailsMedium") val thumbnailsMedium: String,
    @param:Json(name = "thumbnailsHigh") val thumbnailsHigh: String,
    @param:Json(name = "url") val url: String
) : Parcelable

enum class MediaServerType {
    FLUSSONIC,
    NIMBLE,
    MACROSCOP,
    FORPOST,
    SESAMEWARE;

    companion object {
        const val MEDIA_TYPE_FLUSSONIC = "flussonic"
        const val MEDIA_TYPE_NIMBLE = "nimble"
        const val MEDIA_TYPE_MACROSCOP = "macroscop"
        const val MEDIA_TYPE_FORPOST = "forpost"
        const val MEDIA_TYPE_SESAMEWARE = "sesameware"
    }
}

@Parcelize
data class CCTVDataTree(
    @param:Json(name = "groupId") val groupId: Int = DEFAULT_GROUP_ID,
    @param:Json(name = "groupName") val groupName: String?,
    @param:Json(name = "type") val _type: String? = CCTVRepresentationType.TYPE_MAP,
    @param:Json(name = "childGroups") val childGroups: List<CCTVDataTree>?,
    @param:Json(name = "cameras") val cameras: List<CCTVData>?
) : Parcelable {
    val type: CCTVRepresentationType
        get() {
            return when (_type) {
                CCTVRepresentationType.TYPE_LIST -> CCTVRepresentationType.LIST
                else -> CCTVRepresentationType.MAP
            }
        }

    companion object {
        const val DEFAULT_GROUP_ID = -1
    }
}

enum class CCTVRepresentationType {
    MAP,
    LIST;

    companion object {
        const val TYPE_MAP = "map"
        const val TYPE_LIST = "list"
    }
}
