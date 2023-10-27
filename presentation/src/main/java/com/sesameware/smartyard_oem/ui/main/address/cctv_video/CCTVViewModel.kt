package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sesameware.data.DataModule
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
import com.sesameware.domain.model.response.CCTVDataTree
import com.sesameware.domain.model.response.CCTVRepresentationType
import com.sesameware.domain.model.response.CCTVViewTypeType
import com.sesameware.domain.model.response.MediaServerType
import com.sesameware.domain.model.response.RangeObject
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.VideoCameraModelP
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
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
    val cameraGroups = state.getLiveData<CCTVDataTree?>(cameraGroups_Key, null)
    val chosenIndex = state.getLiveData<Int?>(chosenIndex_Key, null)
    val chosenGroup = state.getLiveData<Int?>(chosenGroup_Key, null)
    var chosenGroupName: String? = null
    val chosenCamera = state.getLiveData<CCTVData?>(chosenCamera_Key, null)
    var initialThumb: Bitmap? = null
    var stateFullScreen = MutableLiveData<Boolean>()
    var closedRangeCalendar = MutableLiveData<Event<ClosedRange<LocalDate>>>()

    var endDate: LocalDate = LocalDate.now(ZoneId.of(DataModule.serverTz))
    var startDate: LocalDate = endDate.minusDays(minusDate)

    //доступные интервалы архива для выбранной камеры
    var availableRanges = mutableListOf<AvailableRange>()

    fun fullScreen(flag: Boolean) {
        stateFullScreen.value = flag
    }

    var currentTabId = ONLINE_TAB_POSITION

    private fun getDateTime(s: Long): String? {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = Date(s * 1000)
            sdf.format(date)
        } catch (e: Exception) {
            e.toString()
        }
    }

    private fun loadPeriod() {
        viewModelScope.withProgress({ false }, null) {
            chosenCamera.value?.let {
                availableRanges.clear()
                val loadPeriods = try {
                    when (it.serverType) {
                        MediaServerType.FORPOST,
                        MediaServerType.MACROSCOP,
                        MediaServerType.NIMBLE -> cctvInteractor.ranges(it.id)?.first()?.ranges ?: emptyList()
                        else -> {
                            val url = it.url + "/recording_status.json?from=1525186456&token=" + it.token
                            cctvInteractor.loadPeriods(url)?.first()?.ranges ?: emptyList()
                        }
                    }
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
                        Instant.ofEpochSecond(period.from.toLong()).atZone(ZoneId.of(DataModule.serverTz)).toLocalDateTime(),
                        Instant.ofEpochSecond(period.from.toLong() + period.duration.toLong()).atZone(ZoneId.of(DataModule.serverTz)).toLocalDateTime()
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
            cctvInteractor.getCCTV(model.houseId)?.let {

                //для теста имитация просроченного токена
                /*it.forEach { item ->
                    item.token = "qqq"
                }*/

                state[cameraList_Key] = it.filter { camera ->
                    camera.latitude != null && camera.longitude != null  // игнорируем камеры с неуказанными координатами
                }
                state[cctvModel_Key] = model
                onComplete()
            }
        }
    }

    fun getCamerasTree(model: VideoCameraModelP, onComplete: listenerEmpty = {}) {
        viewModelScope.withProgress {
            cctvInteractor.getCCTVTree(model.houseId)?.let {
                state[cctvModel_Key] = model
                state[cameraGroups_Key] = it
                onComplete()
            }
        }
    }

    private fun findGroupById(data: CCTVDataTree?, groupId: Int): CCTVDataTree? {
        data?.let {group ->
            if (group.groupId == groupId) {
                return group
            } else {
                group.childGroups?.forEach() {child ->
                    val found = findGroupById(child, groupId)
                    if (found != null) {
                        return found
                    }
                }
            }
        }

        return null
    }

    fun getCameraList(cameras: List<CCTVData>, groupType: CCTVRepresentationType = CCTVRepresentationType.MAP, onComplete: listenerEmpty = {}) {
        state[cameraList_Key] = cameras.filter { camera ->
            when (groupType) {
                CCTVRepresentationType.MAP -> camera.latitude != null && camera.longitude != null  // игнорируем камеры с неуказанными координатами
                else -> true
            }
        }
        onComplete()
    }

    fun refreshCameras(model: VideoCameraModelP, onComplete: listenerEmpty = {}) {
        Timber.d("__Q__   call refreshCameras")
        when (DataModule.providerConfig.cctvView) {
            CCTVViewTypeType.TREE -> {
                getCamerasTree(model) {
                    Timber.d("__Q__   call findGroupById    ${chosenGroup.value}")
                    findGroupById(cameraGroups.value, chosenGroup.value ?: CCTVDataTree.DEFAULT_GROUP_ID)?.let { group ->
                        Timber.d("__Q__   found group  ${group.groupName}")
                        getCameraList(group.cameras ?: listOf(), group.type)
                        chosenIndex.value?.let { it1 -> chooseCamera(it1) }
                        onComplete()
                    }
                }
            }
            else -> {
                viewModelScope.withProgress {
                    mPreferenceStorage.xDmApiRefresh = true
                    cctvInteractor.getCCTV(model.houseId)?.let {
                        state[cameraList_Key] = it.filter { camera ->
                            camera.latitude != null && camera.longitude != null  // игнорируем камеры с неуказанными координатами
                        }
                        state[cctvModel_Key] = model
                        chosenIndex.value?.let { it1 -> chooseCamera(it1) }
                        onComplete()
                    }
                }
            }
        }
    }

    fun chooseCamera(index: Int) {
        Timber.d("__Q__  call chooseCamera with index = $index")
        state[chosenIndex_Key] = index
        cameraList.value?.get(index)?.let { camera ->
            state[chosenCamera_Key] = camera
            downloadMaskImage(camera.preview)
            loadPeriod()
        }
    }

    fun chooseGroup(index: Int, groupName: String? = null) {
        state[chosenGroup_Key] = index
        chosenGroupName = groupName
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
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        private const val cctvModel_Key = "cctvModel_Key"
        private const val cameraList_Key = "cameraList_Key"
        private const val cameraGroups_Key = "cameraGroups_Key"
        private const val chosenIndex_Key = "chosenIndex_Key"
        private const val chosenCamera_Key = "chosenCamera_Key"
        private const val chosenGroup_Key = "chosenGroup_Key"

        const val ONLINE_TAB_POSITION = 0
        const val ARCHIVE_TAB_POSITION = 1
        private const val minusDate = 6L

        @Throws(Exception::class)
        fun downloadPreview(url: String): Bitmap? {
            if (url.contains(".mp4")) {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .head()
                    .url(url)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(url, hashMapOf())
                        val result = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        retriever.release()
                        return result
                    } else {
                        Timber.d("debug_dmm    get MP4 error: ${response.code} ${response.message}")
                    }
                }

                return null
            }

            val client = OkHttpClient()
            var imageUrl = url

            //forpost
            if (url.contains("/GetTranslationURL")) {
                val bodyBuilder = FormBody.Builder()
                url.toHttpUrlOrNull()?.let { forpostUrl ->
                    var hasFormat = false
                    for (i in 0 until forpostUrl.querySize) {
                        val paramName = forpostUrl.queryParameterName(i)
                        var paramValue = forpostUrl.queryParameterValue(i) ?: ""
                        if (paramName.lowercase() == "format") {
                            hasFormat = true
                            paramValue = "JPG"
                        }
                        bodyBuilder.add(paramName, paramValue)
                    }
                    if (!hasFormat) {
                        bodyBuilder.add("format", "JPG")
                    }

                    val request = Request.Builder()
                        .url(forpostUrl.toString().split("?", limit = 1)[0])
                        .post(bodyBuilder.build())
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = JSONObject(response.body!!.string())
                            imageUrl = json.optString("URL", "")
                        }
                    }
                }
            }

            val request = Request.Builder()
                .url(imageUrl)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return BitmapFactory.decodeStream(response.body!!.byteStream())
                }
            }

            return null
        }
    }
}
