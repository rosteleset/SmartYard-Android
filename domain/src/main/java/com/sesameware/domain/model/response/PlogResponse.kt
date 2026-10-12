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
    @param:Json(name = "date") val _date: String, // дата и время события
    @param:Json(name = "uuid") val uuid: String,
    @param:Json(name = "image") val image: String?,
    @param:Json(name = "flatId") var flatId: Int? = null, // идентификатор квартиры
    @param:Json(name = "objectId") val objectId: Int, // идентификатор объекта (домофона)
    @param:Json(name = "objectType") val objectType: Int, // тип объекта (0 - домофон)
    @param:Json(name = "objectMechanizma") val objectMechanizma: Int, // идентификатор нагрузки (двери)
    @param:Json(name = "mechanizmaDescription") val mechanizmaDescription: String, // описание нагрузки (двери)
    @param:Json(name = "houseId") val houseId: Int? = null, // идентификатор дома
    @param:Json(name = "entranceId") val entranceId: Int? = null, // идентификатор входа
    @param:Json(name = "cameraId") val cameraId: Int? = null, // идентификатор камеры
    @param:Json(name = "event") val eventType: Int, // тип события
    @param:Json(name = "preview") val preview: String? = null, // url картинки
    @param:Json(name = "previewType") val previewType: Int, // тип картинки (0 - нет, 1 - flussonic, 2 - FRS, 3 - base64)
    @param:Json(name = "detail") val detail: String? = null, // детализация (старый вариант, не используем)
    @param:Json(name = "detailX") var detailX: DetailX? = null, // детализация события
    var address: String = "", // адрес события для показа
    var frsEnabled: Boolean = false // доступна ли FRS
) {
    val date: LocalDateTime
        get() = LocalDateTime.parse(_date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    // расширенная детализация, структура до конца не определена
    data class DetailX(
        @param:Json(name = "opened") val _opened: String? = null, // была ли открыта дверь
        @param:Json(name = "key") val key: String? = null, // ключ, которым открыли дверь
        @param:Json(name = "phone") val phone: String? = null, // телефон
        @param:Json(name = "faceId") var faceId: String?, // идентификатор дескриптора лица
        @param:Json(name = "groupId") var groupId: Int? = null, // идентификатор группы
        @param:Json(name = "groupName") var groupName: String? = null, // имя группы
        @param:Json(name = "code") val code: String? = null, // код, которым открыли дверь
        @param:Json(name = "phone_from") val phoneFrom: String? = null, // телефон
        @param:Json(name = "phone_to") val phoneTo: String? = null, // телефон
        @param:Json(name = "flags") val flags: MutableList<String>? = null, // список флагов к событию
        @param:Json(name = "face") val face: Face? = null, // данные о лице в кадре
        @param:Json(name = "vehicle") val vehicle: Vehicle? = null // информация по распознанному автомобильному номеру
    ) {
        val opened: Boolean?
            get() = if (_opened == null) null else _opened == "t"

        // координаты лица в кадре
        data class Face(
            @param:Json(name = "left") val left: Int, // левый верхний угол
            @param:Json(name = "top") val top: Int,
            @param:Json(name = "width") val width: Int, // ширина
            @param:Json(name = "height") val height: Int // высота
        )

        data class Vehicle(
            @param:Json(name = "vehicleBox") val vehicleBox: MutableList<Int>? = null, // координаты левого верхнего и правого нижнего угла прямоугольной области, определяющей положение автомобиля
            @param:Json(name = "plateKeyPoints") val plateKeyPoints: MutableList<Int>? = null, // координаты четырёхугольника, определяющие положение автомобильного номера
            @param:Json(name = "plateNumber") val plateNumber: String? // автомобильный номер
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
        const val EVENT_RESERVED_FOR_FUTURE = 8
        const val EVENT_OPEN_GATES_BY_VEHICLE = 9

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
        EVENT_OPEN_GATES_BY_CALL,
        EVENT_RESERVED_FOR_FUTURE,
        EVENT_OPEN_GATES_BY_VEHICLE)
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
