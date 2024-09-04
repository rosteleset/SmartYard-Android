package com.sesameware.domain.model.response

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import com.squareup.moshi.Json
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

typealias PlogResponse = ApiResult<List<Plog>>?

/**
 * Событие домофона
 */
data class Plog(
    @Json(name = "date") val _date: String, // дата и время события
    @Json(name = "uuid") val uuid: String,
    @Json(name = "image") val image: String?,
    @Json(name = "flatId") val flatId: Int? = null, // идентификатор квартиры
    @Json(name = "objectId") val objectId: Int, // идентификатор объекта (домофона)
    @Json(name = "objectType") val objectType: Int, // тип объекта (0 - домофон)
    @Json(name = "objectMechanizma") val objectMechanizma: Int, // идентификатор нагрузки (двери)
    @Json(name = "mechanizmaDescription") val mechanizmaDescription: String, // описание нагрузки (двери)
    @Json(name = "houseId") val houseId: Int? = null, // идентификатор дома
    @Json(name = "entranceId") val entranceId: Int? = null, // идентификатор входа
    @Json(name = "cameraId") val cameraId: Int? = null, // идентификатор камеры
    @Json(name = "event") val eventType: Int, // тип события
    @Json(name = "preview") val preview: String? = null, // url картинки
    @Json(name = "previewType") val previewType: Int, // тип картинки (0 - нет, 1 - flussonic, 2 - FRS, 3 - base64)
    @Json(name = "detail") val detail: String? = null, // детализация (старый вариант, не используем)
    @Json(name = "detailX") val detailX: DetailX? = null, // детализация события
    var address: String = "", // адрес события для показа
    var frsEnabled: Boolean = false // доступна ли FRS
) {
    val date: LocalDateTime
        get() = LocalDateTime.parse(_date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    // расширенная детализация, структура до конца не определена
    data class DetailX(
        @Json(name = "opened") val _opened: String? = null, // была ли открыта дверь
        @Json(name = "key") val key: String? = null, // ключ, которым открыли дверь
        @Json(name = "phone") val phone: String? = null, // телефон
        @Json(name = "faceId") val faceId: String?, // идентификатор дескриптора лица
        @Json(name = "code") val code: String? = null, // код, которым открыли дверь
        @Json(name = "phone_from") val phoneFrom: String? = null, // телефон
        @Json(name = "phone_to") val phoneTo: String? = null, // телефон
        @Json(name = "flags") val flags: MutableList<String>? = null, // список флагов к событию
        @Json(name = "face") val face: Face? = null // данные о лице в кадре
    ) {
        val opened: Boolean?
            get() = if (_opened == null) null else _opened == "t"

        // координаты лица в кадре
        data class Face(
            @Json(name = "left") val left: Int, // левый верхний угол
            @Json(name = "top") val top: Int,
            @Json(name = "width") val width: Int, // ширина
            @Json(name = "height") val height: Int // высота
        )
    }

    companion object {
        const val EVENT_DOOR_PHONE_CALL_UNANSWERED = 1
        const val EVENT_DOOR_PHONE_CALL_ANSWERED = 2
        const val EVENT_OPEN_BY_KEY = 3
        const val EVENT_OPEN_FROM_APP = 4
        const val EVENT_OPEN_BY_FACE = 5
        const val EVENT_OPEN_BY_CODE = 6
        const val EVENT_OPEN_GATES_BY_CALL = 7

        const val NO_PREVIEW = 0
        const val PREVIEW_FLUSSONIC = 1
        const val PREVIEW_FRS = 2
        const val PREVIEW_BASE64 = 3

        const val FLAG_CAN_LIKE = "canLike"
        const val FLAG_CAN_DISLIKE = "canDislike"
        const val FLAG_LIKED = "liked"
    }

    @IntDef(EVENT_DOOR_PHONE_CALL_UNANSWERED,
        EVENT_DOOR_PHONE_CALL_ANSWERED,
        EVENT_OPEN_BY_KEY,
        EVENT_OPEN_FROM_APP,
        EVENT_OPEN_BY_FACE,
        EVENT_OPEN_BY_CODE,
        EVENT_OPEN_GATES_BY_CALL)
    @Retention(AnnotationRetention.SOURCE)
    annotation class IntercomEvent

    @IntDef(NO_PREVIEW,
        PREVIEW_FLUSSONIC,
        PREVIEW_FRS,
        PREVIEW_BASE64)
    @Retention(AnnotationRetention.SOURCE)
    annotation class PreviewType

    @StringDef(FLAG_CAN_LIKE,
        FLAG_CAN_DISLIKE,
        FLAG_LIKED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LikeFlag
}


