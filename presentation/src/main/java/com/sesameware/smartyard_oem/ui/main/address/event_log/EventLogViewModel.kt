package com.sesameware.smartyard_oem.ui.main.address.event_log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.FRSInteractor
import com.sesameware.domain.model.response.MediaServerType
import com.sesameware.domain.model.response.Plog
import com.sesameware.domain.model.response.targetZoneId
import com.sesameware.smartyard_oem.GenericViewModel
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import timber.log.Timber

data class Flat(
    val flatId: Int,
    val flatNumber: String,
    val frsEnabled: Boolean
)

data class EventDayData(
    val day: LocalDate,
    val eventCount: Int
)

data class DoorphoneData(
    val url: String,
    val token: String,
    val serverType: MediaServerType
) {
    fun getHlsAt(time: LocalDateTime, durationSeconds: Long): String {
        val zoned = time.atZone(targetZoneId).withZoneSameInstant(ZoneId.systemDefault())
        val timeStamp = DateTimeUtils.toSqlTimestamp(zoned.toLocalDateTime()).time / 1000
        return when (serverType) {
            MediaServerType.NIMBLE -> "$url/playlist_dvr_range-$timeStamp-$durationSeconds.m3u8?wmsAuthSign=$token"
            else -> "$url/index-$timeStamp-$durationSeconds.m3u8?token=$token"
        }
    }
}

class EventLogViewModel(
    private val addressInteractor: AddressInteractor,
    private val frsInteractor: FRSInteractor
) : GenericViewModel() {

    var address: String = ""  //адрес дома
    var flatsAll = listOf<Flat>()  //список всех доступных пользователю квартир

    private var isLoading = false
    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean>
        get() = _progress

    //для фильтра журнала событий
    var filterEventType = mutableSetOf(
        Plog.EVENT_DOOR_PHONE_CALL_ANSWERED,
        Plog.EVENT_DOOR_PHONE_CALL_UNANSWERED,
        Plog.EVENT_OPEN_BY_KEY,
        Plog.EVENT_OPEN_FROM_APP,
        Plog.EVENT_OPEN_BY_FACE,
        Plog.EVENT_OPEN_BY_CODE,
        Plog.EVENT_OPEN_GATES_BY_CALL)  //по-умолчанию все типы событий

    private var cacheEvents = hashMapOf<String, MutableList<Plog>>()

    private var _eventDaysFilter = mutableListOf<EventDayData>()  //список дат по событиям фильтра
    var eventDaysFilter = mutableListOf<EventDayData>()
    var filterFlat: Flat? = null  //по умолчанию все квартиры
    private var _eventsByDaysFilter = hashMapOf<LocalDate, MutableList<Plog>>()  //все события по датам фильтра
    var eventsByDaysFilter = hashMapOf<LocalDate, MutableList<Plog>>()

    private var _lastLoadedDayFilterIndex = -1
    var lastLoadedDayFilterIndex = MutableLiveData(-1)  //индекс последнего запрошенного дня с событиями из eventDaysFilter

    var currentEventItem: Pair<LocalDate, Int>? = null
    var currentEventDayFilter: LocalDate? = null

    var camMapData = hashMapOf<Int, DoorphoneData>()

    var faceIdToUrl = hashMapOf<Int, String>()

    init {
        camMap()
        getAllFaces()
    }

    private fun camMap() {
        camMapData.clear()
        viewModelScope.withProgress(progress = null) {
            val data = hashMapOf<Int, DoorphoneData>()
            val result = addressInteractor.camMap()
            result?.data?.forEach {
                data[it.id] = DoorphoneData(it.url, it.token, it.serverType)
            }
            withContext(Dispatchers.Main) {
                camMapData = HashMap(data)
            }
        }
    }

    fun getAllFaces() {
        viewModelScope.withProgress(progress = null) {
            val q = hashMapOf<Int, String>()
            flatsAll.forEach {
                val res = frsInteractor.listFaces(it.flatId)
                res?.data?.forEach { faceData ->
                    q[faceData.faceId.toInt()] = faceData.faceImage
                }
            }
            withContext(Dispatchers.Main) {
                faceIdToUrl = HashMap(q)
            }
        }
    }

    fun getEventItemCountTillDay(day: LocalDate): Int {
        var count = 0
        eventDaysFilter.forEach {
            if (it.day >= day) {
                count += it.eventCount
            } else {
                return count
            }
        }

        return count
    }

    fun getEventDayFilterIndex(day: LocalDate): Int {
        eventDaysFilter.forEachIndexed { index, eventDayData ->
            if (eventDayData.day <= day) {
                return index
            }
        }

        return -1
    }

    fun getEventDaysFilter() {
        if (isLoading) {
            return
        }

        isLoading = true

        eventDaysFilter.clear()
        eventsByDaysFilter.clear()
        _lastLoadedDayFilterIndex = -1

        _progress.postValue(true)
        viewModelScope.withProgress(progress = null /* _progress */) {
            _eventDaysFilter.clear()
            _eventsByDaysFilter.clear()
            var flats = mutableListOf<Flat>()
            if (filterFlat == null) {
                flats = flatsAll.toMutableList()
            } else {
                flats.add(filterFlat!!)
            }

            val dayToEventCount = hashMapOf<LocalDate, Int>()

            flats.forEach {flat ->
                try {
                    addressInteractor.plogDays(flat.flatId, filterEventType)?.let {response ->
                        response.data.forEach {plogDays ->
                            val d = LocalDate.parse(plogDays.day, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            val ec = plogDays.events
                            if (dayToEventCount.containsKey(d)) {
                                var dd = dayToEventCount[d] ?: 0
                                dd += ec
                                dayToEventCount[d] = dd
                            } else {
                                dayToEventCount[d] = ec
                            }
                        }
                    }
                } catch (e: Throwable) {}
            }

            dayToEventCount.keys.sortedDescending().forEach {
                _eventDaysFilter.add(EventDayData(it, dayToEventCount[it] ?: 0))
            }

            withContext(Dispatchers.Main) {
                eventDaysFilter = _eventDaysFilter.toMutableList()
                Timber.d("__Q__ eventDaysFilter = $eventDaysFilter")
                isLoading = false
                _progress.postValue(false)
            }

            getMoreEvents()
        }
    }

    fun getMoreEvents() {
        if (isLoading) {
            return
        }

        Timber.d("__Q__ call getMoreEvents")

        var eventCount = 0
        val days = mutableListOf<LocalDate>()
        var canBreak = false
        var canBreak2 = (currentEventDayFilter == null)
        while (_lastLoadedDayFilterIndex < _eventDaysFilter.size - 1) {
            _lastLoadedDayFilterIndex++

            if (!canBreak2) {
                currentEventDayFilter?.let { dayToShow ->
                    if (_eventDaysFilter[_lastLoadedDayFilterIndex].day <= dayToShow) {
                        canBreak2 = true
                        eventCount = 0
                    }
                }
            }

            eventCount += _eventDaysFilter[_lastLoadedDayFilterIndex].eventCount
            days.add(_eventDaysFilter[_lastLoadedDayFilterIndex].day)
            if (eventCount > EVENT_COUNT_CHUNK) {
                canBreak = true
            }

            if (canBreak && canBreak2) {
                break
            }
        }

        getEvents(days)
    }

    fun getMoreEvents(days: List<LocalDate>) {
        if (isLoading) {
            return
        }

        Timber.d("__Q__ call getMoreEvents2")
        if (days.isNotEmpty()) {
            _lastLoadedDayFilterIndex += days.size
        }

        if (_lastLoadedDayFilterIndex > _eventDaysFilter.size - 1) {
            _lastLoadedDayFilterIndex = _eventDaysFilter.size - 1
        }

        getEvents(days)
    }

    private fun getEvents(days: List<LocalDate>) {
        Timber.d("__Q__ getEvents: $days")
        isLoading = true
        viewModelScope.withProgress(progress = _progress) {
            days.forEach {day ->
                _eventsByDaysFilter[day] = mutableListOf()

                var flats = mutableListOf<Flat>()
                if (filterFlat == null) {
                    flats = flatsAll.toMutableList()
                } else {
                    flats.add(filterFlat!!)
                }

                flats.forEach {flat ->
                    try {
                        val dayFormat = day.format(DateTimeFormatter.ofPattern(DAY_FORMAT))
                        val cacheKey = "${dayFormat}_${flat.flatId}"
                        if (cacheEvents.containsKey(cacheKey) && day != LocalDate.now()) {
                            Timber.d("__Q__ from cache $cacheKey")
                            cacheEvents[cacheKey]?.let {logDataList ->
                                logDataList.forEach {logElement ->
                                    if (filterEventType.contains(logElement.eventType)) {
                                        _eventsByDaysFilter.getOrPut(day) {mutableListOf()}
                                            .add(logElement)
                                    }
                                }
                            }
                        } else {
                            Timber.d("__Q__ from server $cacheKey")
                            cacheEvents[cacheKey] = mutableListOf()
                            addressInteractor.plog(flat.flatId, dayFormat)?.let {plogResponse ->
                                plogResponse.data.forEach {plog ->
                                    if (flat.flatNumber.isNotEmpty()) {
                                        plog.address = address + ", кв. ${flat.flatNumber}"
                                    } else {
                                        plog.address = address
                                    }
                                    plog.frsEnabled = flat.frsEnabled
                                    cacheEvents.getOrPut(cacheKey) {mutableListOf()}.add(plog)
                                    if (filterEventType.contains(plog.eventType)) {
                                        _eventsByDaysFilter.getOrPut(day) {mutableListOf()}
                                            .add(plog)
                                    }
                                }
                            }
                        }
                    } catch(e: Throwable) {
                        
                    }
                }

                _eventsByDaysFilter[day]?.sortByDescending {eventLogData ->
                    eventLogData.date
                }
            }

            withContext(Dispatchers.Main) {
                eventsByDaysFilter = HashMap(_eventsByDaysFilter)
                lastLoadedDayFilterIndex.value = _lastLoadedDayFilterIndex
                isLoading = false
            }
        }
    }

    fun dislike(uuid: String) {
        viewModelScope.withProgress(progress = null) {
            frsInteractor.disLike(uuid, null, null)
        }
    }

    fun like(uuid: String) {
        viewModelScope.withProgress(progress = null) {
            frsInteractor.like(uuid, "")
        }
    }

    companion object {
        const val DAY_FORMAT = "yyyy-MM-dd"
        const val EVENT_COUNT_CHUNK = 10
        const val EVENT_VIDEO_BACK_SECONDS = 1 * 60L
        const val EVENT_VIDEO_DURATION_SECONDS = EVENT_VIDEO_BACK_SECONDS * 2
        const val SEEK_STEP = 10_000L  // интервал перемотки в миллисекундах видео в деталях события при двойном тапе
        const val PAUSE = "Пауза"
        const val PLAYING = "Видео"
        const val SCREENSHOT = "Кадр события"
        const val EVENT_LOG_KEEPING_MONTHS = 6L  // период хранения журнала событий в месяцах
    }
}
