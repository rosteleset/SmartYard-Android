package ru.madbrains.smartyard.ui.player

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AddressInteractor
import ru.madbrains.domain.interactors.CCTVInteractor
import ru.madbrains.domain.model.response.CCTVData
import ru.madbrains.domain.model.response.Door
import ru.madbrains.domain.model.response.Plog
import ru.madbrains.domain.utils.listenerEmpty
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModelP
import ru.madbrains.smartyard.ui.player.ExoPlayerFragment.Companion.HEIGHT_TIME_SECTOR
import ru.madbrains.smartyard.ui.player.ExoPlayerFragment.Companion.SCALE_X1
import ru.madbrains.smartyard.ui.player.ExoPlayerFragment.Companion.SCALE_X2
import timber.log.Timber
import java.util.Date
import ru.madbrains.domain.model.response.RangeObject
import java.io.Serializable
import java.util.Locale
import kotlin.math.max
import kotlin.math.min


enum class ScaleType(val scale: Int) {
    SCALEX1(15),
    SCALEX2(2)
}


@Parcelize
data class TimeLineItem(
    val id: Int,
    val name: String,
    var rangeTime: MutableList<MutableMap<Long, MutableList<NoDvrPeriods?>>>
) : Parcelable

//@Parcelize
//data class RangeTime(
//    val cellTime: Long,
//    val noDvrPeriods: MutableList<NoDvrPeriods>
//):Parcelable

@Parcelize
data class NoDvrPeriods(
    val first: Long,
    val second: Long
) : Parcelable

@Parcelize
data class CCTVDataItem(
    val id: Int,
    var elementsCount: Int,
    val url: String,
    val name: String,
    val token: String,
    val flatIds: List<String>,
    val doors: List<Door>?,
) : Parcelable {
    val hls: String get() = "$url/index.m3u8?token=$token"
    val previewUrl: String get() = "$url/preview.mp4?token=$token"
    val timeStamp: String get() = "$url/recording_status.json?from=1525186456&token=$token"
    fun archiveHls(duration: Long) = "$url/index-$duration-86400.m3u8?token=$token"
}

@Parcelize
data class EventsCamera(
    val id: Int,
    val event: List<Plog>?,
) : Parcelable

class ExoPlayerViewModel(
    private val state: SavedStateHandle,
    private val mPreferenceStorage: PreferenceStorage,
    private val cctvInteractor: CCTVInteractor,
    private val addressInteractor: AddressInteractor
) : GenericViewModel() {
//    val cctvModel = state.getLiveData<VideoCameraModelP>(cctvModel_Key, null)
    val cameraList = state.getLiveData<List<CCTVDataItem>?>(cameraList_Key, null)
    val scaleHeight = state.getLiveData<Int>(scaleHeight_Key, HEIGHT_TIME_SECTOR)
    val timeLineItemX1 = state.getLiveData<TimeLineItem?>(timeLineItem_Key, null)
    val timeLineItemX2 = state.getLiveData<TimeLineItem?>(timeLineItem_X2_Key, null)
    val scale = state.getLiveData<Int>(scale_Key, SCALE_X1)

    val calendar = state.getLiveData<Long>(calendarDate_key, Calendar.getInstance().timeInMillis)
    val timeInTimer = state.getLiveData<Long>(timeInTimer_Key, Calendar.getInstance().timeInMillis)
    val chosenCamera = state.getLiveData<CCTVData?>(chosenCamera_Key, null)
    val chosenIndex = state.getLiveData<Int?>(chosenIndex_Key, null)

    val eventsCamera = state.getLiveData<List<EventsCamera>?>(eventsCamera_Key, null)


    fun setScale(scale: Int) = state.set(scale_Key, scale)
    fun setCalendar(date: Long) = state.set(calendarDate_key, date)
    fun setScaleHeight(height: Int, onComplete: listenerEmpty? = null) {
        state.set(scaleHeight_Key, height)
        if (onComplete != null) {
            onComplete()
        }
    }

    fun setTimeInTimer(time: Long) = state.set(timeInTimer_Key, time)
    fun chooseCamera(index: Int) = state.set(chosenIndex_Key, index)
    fun getTimeLineItem() = if (scale.value == SCALE_X1) timeLineItemX1 else timeLineItemX2
    fun getCameras(model: VideoCameraModelP, camId: Int, onComplete: listenerEmpty) {
        val list = mutableListOf<CCTVDataItem>()
        viewModelScope.withProgress {
            cctvInteractor.getCCTV(model.houseId)?.let {
                it.forEach { data ->
                    list.add(
                        CCTVDataItem(
                            data.id,
                            0,
                            data.url,
                            data.name,
                            data.token,
                            data.flatIds ?: listOf(""),
                            data.doors
                        )
                    )
                }
                state.set(cameraList_Key, list)
                state.set(cctvModel_Key, model)
                cameraList.value?.indices?.forEach { index ->
                    if (cameraList.value?.get(index)?.id == camId) {
                        chooseCamera(index)
                    }
                }
                loadPeriod()
                onComplete()
            }
        }
    }

    fun getPlog(date: Long) {
        val events = mutableListOf<EventsCamera>()
        viewModelScope.withProgress {
            cameraList.value?.get(chosenIndex.value!!)?.flatIds?.forEach { ids ->
                addressInteractor.plogDays(ids.toInt()).let { it1 ->
                    it1?.data?.forEach { it2 ->
                        if (it2.day == dateFormat.format(Date(date))) {
                            addressInteractor.plog(ids.toInt(), dateFormat.format(Date(date)))
                                ?.let {
                                    events.add(EventsCamera(ids.toInt(), it.data))
                                }
                        }
                    }
                }
            }
            state.set(eventsCamera_Key, events)
            events.clear() //Не трогать это надо!!! так как по неизвестной мне причине при открытие нового фрагмента данные в events не пустые это приводит к тому что на другом адресе писутствуют события с прошлого фрагмента
        }
    }


    @SuppressLint("SimpleDateFormat")
    fun createRanges(
        scale: Int,
        range: Long,
        gaps: MutableList<Pair<Long, Long>>
    ): MutableMap<Long, MutableList<NoDvrPeriods?>> {
        val rangeMin = range / 60
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance(TIME_ZONE)
        calendar.timeInMillis = currentTimeMillis
        val minute = calendar.get(Calendar.MINUTE)
        val remainder = minute % scale
        val roundedMinutes = (minute - remainder) + scale
        calendar.set(Calendar.MINUTE, roundedMinutes)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val mapList: MutableMap<Long, MutableList<NoDvrPeriods?>> = mutableMapOf()

        for (i in 0..rangeMin step scale.toLong()) {
            val dvr = createDvrPeriods(calendar.timeInMillis, scale, gaps)
            mapList.put(calendar.timeInMillis, dvr)
            calendar.add(Calendar.MINUTE, -scale)
        }

        val currentEmptyTime = scale - remainder
//        Timber.d("currentEmptyTime ${mapList}")

        return mapList
    }

    private fun createDvrPeriods(
        time: Long,
        scale: Int,
        gaps: MutableList<Pair<Long, Long>>
    ): MutableList<NoDvrPeriods?> {
        val timeStart = time / 1000
        val timeEnd = timeStart - (scale * 60)
        val listDvr: MutableList<NoDvrPeriods?> = mutableListOf()

        for (gap in gaps) {
            val start = min(gap.first, timeStart)
            val end = max(gap.second, timeEnd)
            if (start >= end && start != end && timeStart > start) {
                listDvr.add(NoDvrPeriods(start, end))
            }
        }
        return listDvr
    }

    fun loadPeriod(scale: Int = this.scale.value!!, onComplete: listenerEmpty? = null) {
        viewModelScope.withProgress {
            try {
                val i = chosenIndex.value
                if (i != null && !cameraList.value.isNullOrEmpty()) {
                    cameraList.let {
                        val cl = it.value!![i]
                        val url = cl.timeStamp
                        //Получаем временой период для камеры которая выбрана
                        val loadPeriodsRange = cctvInteractor.loadPeriods(url)
                        val gaps = findGaps(loadPeriodsRange?.get(0)!!.ranges)

                        val lastPeriodsRange = loadPeriodsRange?.last()?.ranges ?: emptyList()
                        val lastPeriod = lastPeriodsRange[0].from
                        val currentTimestamp =
                            Instant.now().truncatedTo(ChronoUnit.SECONDS).epochSecond
                        val range = currentTimestamp - lastPeriod
                        val ranges = createRanges(SCALE_X1, range, gaps)
                        val rangesX2 = createRanges(SCALE_X2, range, gaps)

                        if (ranges.isNotEmpty()) {
                            val rangeTime =
                                mutableListOf<MutableMap<Long, MutableList<NoDvrPeriods?>>>()
                            ranges.forEach { (key, value) ->
                                rangeTime.add(mutableMapOf(key to value))
                            }
                            val timeItem = TimeLineItem(cl.id, cl.name, rangeTime = rangeTime)
                            state.set(timeLineItem_Key, timeItem)
                        }
                        if (rangesX2.isNotEmpty()) {
                            val rangeTimeX2 =
                                mutableListOf<MutableMap<Long, MutableList<NoDvrPeriods?>>>()
                            rangesX2.forEach { (key, value) ->
                                rangeTimeX2.add(mutableMapOf(key to value))
                            }
                            val timeItemX2 = TimeLineItem(cl.id, cl.name, rangeTime = rangeTimeX2)
                            state.set(timeLineItem_X2_Key, timeItemX2)
                        }
                        //callBack
                        if (onComplete != null) {
                            onComplete()
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun findGaps(ranges: List<RangeObject.Range>): MutableList<Pair<Long, Long>> {
        val sortedRanges = ranges.sortedBy { it.from }
        val gaps = mutableListOf<Pair<Long, Long>>()
        for (i in 1 until sortedRanges.size) {
            val previousEnd = sortedRanges[i - 1].from + sortedRanges[i - 1].duration.toLong()
            val currentStart = sortedRanges[i].from.toLong()
            if (currentStart > previousEnd) {
                gaps.add(currentStart to previousEnd)
            }
        }
        return gaps
    }


    companion object {
        val TIME_ZONE = TimeZone.getDefault()

        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ru"))
        val dateTimeFormatForExif = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale("ru"))
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("ru"))
        val dateFormatRu = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val dayWithMountFormatRu = SimpleDateFormat("dd MMMM", Locale("ru"))
        val minuteWithSecondsFormat = SimpleDateFormat("HH:mm:ss", Locale("ru"))
        val minuteFormat = SimpleDateFormat("HH:mm", Locale("ru"))

        private const val cctvModel_Key = "cctvModel_Key"
        private const val cameraList_Key = "cameraList_Key"
        private const val timeInTimer_Key = "timeInTimer_Key"
        private const val chosenIndex_Key = "chosenIndex_Key"
        private const val chosenCamera_Key = "chosenCamera_Key"
        private const val eventsCamera_Key = "eventsCamera_Key"
        private const val timeLineItem_Key = "timeLineItem_Key"
        private const val timeLineItem_X2_Key = "timeLineItem_X2_Key"
        private const val scale_Key = "scale_Key"
        private const val scaleHeight_Key = "scaleHeight_Key"
        private const val calendarDate_key = "calendarDate_key"
        private const val urlImageQueue_key = "urlImageQueue_key"
    }
}
