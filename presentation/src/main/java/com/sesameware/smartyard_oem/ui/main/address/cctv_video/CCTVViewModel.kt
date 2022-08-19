package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.CCTVInteractor
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.domain.model.response.RangeObject
import com.sesameware.domain.model.response.targetZoneId
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.VideoCameraModelP
import java.util.*

@Parcelize
data class AvailableRange(
    val duration: Int,  //Длительность интервала в секундах
    val from: Int,  //Timestamp начала интервала в секундах
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
) : Parcelable

fun isDateInAvailableRanges(date: LocalDate, ranges: List<AvailableRange>): Boolean {
    return ranges.find {
        (date.atStartOfDay() < it.endDate) && (it.startDate < date.plusDays(1).atStartOfDay())
    } != null
}

class CCTVViewModel(
    private val state: SavedStateHandle,
    private val mPreferenceStorage: PreferenceStorage,
    private val cctvInteractor: CCTVInteractor
) : GenericViewModel() {
    val cctvModel = state.getLiveData<VideoCameraModelP?>(cctvModel_Key, null)
    val cameraList = state.getLiveData<List<CCTVData>?>(cameraList_Key, null)
    val chosenIndex = state.getLiveData<Int?>(chosenIndex_Key, null)
    val chosenCamera = state.getLiveData<CCTVData?>(chosenCamera_Key, null)
    var initialThumb: Bitmap? = null
    var stateFullScreen = MutableLiveData<Boolean>()
    var closedRangeCalendar = MutableLiveData<Event<ClosedRange<LocalDate>>>()

    var endDate: LocalDate = LocalDate.now(targetZoneId)
    var startDate: LocalDate = endDate.minusDays(minusDate)

    //доступные интервалы архива для выбранной камеры
    var availableRanges = mutableListOf<AvailableRange>()

    //время последнего запроса списка камер; используется для (не)кэшированного запроса
    private var lastGetCamerasCall: LocalDateTime? = null

    fun fullScreen(flag: Boolean) {
        stateFullScreen.value = flag
    }

    var currentTabId = ONLINE_TAB_POSITION

    private fun getDateTime(s: Long): String? {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = java.util.Date(s * 1000)
            sdf.format(date)
        } catch (e: Exception) {
            e.toString()
        }
    }

    private fun loadPeriod() {
        viewModelScope.withProgress({ false }, null) {
            chosenCamera.value?.let {
                availableRanges.clear()
                val url = it.url + "/recording_status.json?from=1525186456&token=" + it.token
                val loadPeriods = try {
                    cctvInteractor.loadPeriods(url)?.first()?.ranges ?: emptyList()
                } catch (e: Throwable) {
                    emptyList()
                }

                //для теста
                /*val loadPeriods = mutableListOf<RangeObject.Range>()
                loadPeriods.add(RangeObject.Range(10, 1610521200))
                loadPeriods.add(RangeObject.Range(600, 1610522400))
                loadPeriods.add(RangeObject.Range(4200, 1610524500))
                loadPeriods.add(RangeObject.Range(2700, 1610538300))*/

                //заполняем периоды архива, которые есть на сервере
                loadPeriods.forEach {period ->
                    //для теста
                    /*if (availableRanges.size == 0) {
                        period.from = 1602475470
                    }
                    period.from -= 3196800*/
                    
                    availableRanges.add(AvailableRange(period.duration, period.from,
                        Instant.ofEpochSecond(period.from.toLong()).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        Instant.ofEpochSecond(period.from.toLong() + period.duration.toLong()).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    ))
                }

                val maxFrom = loadPeriods.maxByOrNull { (it.from + it.duration) } ?: RangeObject.Range(0, 0)
                val minFrom = loadPeriods.minByOrNull { it.from }?.from ?: 0
                val maxDate = getDateTime((maxFrom.from + maxFrom.duration).toLong())
                val minDate = getDateTime(minFrom.toLong())
                val sdf = org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val localDateMax = LocalDate.parse(maxDate, sdf)
                val localDataMin = LocalDate.parse(minDate, sdf)
                closedRangeCalendar.value = Event(localDataMin.rangeTo(localDateMax))
            }
        }
    }

    fun getCameras(model: VideoCameraModelP, onComplete: listenerEmpty) {
        viewModelScope.withProgress {
            //управление кэшированием запроса
            //этот запрос точно должен быть некэшированным, если предыдущий был дольше, чем NO_CACHE_INTERVAL_HOURS часов назад
            //или предыдущий запрос был до смены токена авторизации камер, который происходит раз в сутки в CAM_TOKEN_RENEW_SCHEDULER_HOUR часов
            lastGetCamerasCall?.let {
                val now = LocalDateTime.now()
                if (it.plusHours(NO_CACHE_INTERVAL_HOURS) < now) {
                    mPreferenceStorage.xDmApiRefresh = true
                    return@let
                }
                if (it.dayOfMonth == now.dayOfMonth
                    && it.hour < CAM_TOKEN_RENEW_SCHEDULER_HOUR && CAM_TOKEN_RENEW_SCHEDULER_HOUR < now.hour) {
                    mPreferenceStorage.xDmApiRefresh = true
                    return@let
                }
                if (it.dayOfMonth < now.dayOfMonth && it.hour < CAM_TOKEN_RENEW_SCHEDULER_HOUR) {
                    mPreferenceStorage.xDmApiRefresh = true
                    return@let
                }
            }
            if (lastGetCamerasCall == null) {
                mPreferenceStorage.xDmApiRefresh = true
            }
            val xDmApiRefresh = mPreferenceStorage.xDmApiRefresh
            cctvInteractor.getCCTV(model.houseId)?.let {
                if (xDmApiRefresh) {
                    lastGetCamerasCall = LocalDateTime.now()
                }
                state.set(cameraList_Key, it.filter { camera ->
                    camera.latitude != null && camera.longitude != null  // игнорируем камеры с неуказанными координатами
                })
                state.set(cctvModel_Key, model)
                //chooseCamera(0)
                onComplete()
            }
        }
    }

    fun chooseCamera(index: Int) {
        state.set(chosenIndex_Key, index)
        cameraList.value?.get(index)?.let { camera ->
            state.set(chosenCamera_Key, camera)
            downloadMaskImage(camera.preview)
            loadPeriod()
        }
    }

    fun setCurrentTabPosition(position: Int) {
        when (position) {
            ONLINE_TAB_POSITION -> currentTabId = ONLINE_TAB_POSITION
            ARCHIVE_TAB_POSITION -> currentTabId = ARCHIVE_TAB_POSITION
        }
    }

    private fun downloadMaskImage(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                initialThumb = downloadPreview(url)
            } catch (e: Throwable) {
            }
        }
    }

    companion object {
        private const val cctvModel_Key = "cctvModel_Key"
        private const val cameraList_Key = "cameraList_Key"
        private const val chosenIndex_Key = "chosenIndex_Key"
        private const val chosenCamera_Key = "chosenCamera_Key"

        const val ONLINE_TAB_POSITION = 0
        const val ARCHIVE_TAB_POSITION = 1
        private const val minusDate = 6L

        //час обновления токена авторизации для камер
        private const val CAM_TOKEN_RENEW_SCHEDULER_HOUR = 4

        //интервал в часах, спустя который запрос списка камер должен быть некэшированным (должен быть меньше 24)
        private const val NO_CACHE_INTERVAL_HOURS = 1L

        @Throws(Exception::class)
        fun downloadPreview(url: String): Bitmap? {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(url, hashMapOf())
            val result =
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            return result
        }
    }
}
