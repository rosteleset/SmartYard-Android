package ru.madbrains.smartyard.ui.main.address.cctv_video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.currentCompositionErrors
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.ImageLoader
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.request.videoFrameMillis
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.component.inject
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.CCTVInteractor
import ru.madbrains.domain.interactors.CameraImageInteractor
import ru.madbrains.domain.model.ImageItem
import ru.madbrains.domain.model.response.CCTVData
import ru.madbrains.domain.model.response.RangeObject
import ru.madbrains.domain.utils.listenerEmpty
import ru.madbrains.smartyard.DiskCache
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.ImageCache
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVDetailFragment.Companion.ARCHIVE_TAB_POSITION
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVDetailFragment.Companion.ONLINE_TAB_POSITION
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModelP
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.locks.Lock

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


data class CacheImage(
    val id: Int,
    val bitmap: Bitmap?
)

class CCTVViewModel(
    private val state: SavedStateHandle,
    private val mPreferenceStorage: PreferenceStorage,
    private val cctvInteractor: CCTVInteractor,
    private val databaseCache: CameraImageInteractor,
) : GenericViewModel() {

    val cctvModel = state.getLiveData<VideoCameraModelP?>(cctvModel_Key, null)
    val cameraList = state.getLiveData<List<CCTVData>?>(cameraList_Key, null)
    val chosenIndex = state.getLiveData<Int?>(chosenIndex_Key, null)
    val chosenId = state.getLiveData<Int?>(chosenId_Key, null)
    val chosenCamera = state.getLiveData<CCTVData?>(chosenCamera_Key, null)
    val favoriteCamera = state.getLiveData<MutableList<Int>?>(favoriteCamera_Key, null)
    var initialThumb: Bitmap? = null
    var isFullScreenMod = state.getLiveData<Boolean>(stateFullScreen_Key, false)
    var isMutePlayerVideo = MutableLiveData<Boolean>() //TODO
    var closedRangeCalendar = MutableLiveData<Event<ClosedRange<LocalDate>>>()
    var updateCalendar = MutableLiveData<Event<ClosedRange<LocalDate>>>()


    //доступные интервалы архива для выбранной камеры
    var availableRanges = mutableListOf<AvailableRange>()
    val bitmapList = mutableListOf<Pair<Int, Bitmap>>()
    fun fullScreen(flag: Boolean) {
        state.set(stateFullScreen_Key, flag)
//        stateFullScreen.value = flag

    }


    fun mutePlayerSound(flag: Boolean) {
        isMutePlayerVideo.value = flag
    }//TODO MUTE

    var currentTabId = ONLINE_TAB_POSITION

    private fun getDateTime(s: Long): String? {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
            val date = java.util.Date(s * 1000)
            sdf.format(date)
        } catch (e: Exception) {
            e.toString()
        }
    }

    fun loadPeriod(boolean: Boolean = true) {
        val withProgress = if (boolean) globalData.progressVisibility else null
        viewModelScope.withProgress(progress = withProgress) {
            chosenCamera.value?.let {
                val url = it.url + "/recording_status.json?from=1525186456&token=" + it.token
                val loadPeriods = cctvInteractor.loadPeriods(url)?.first()?.ranges ?: emptyList()
                //для теста
                /*val loadPeriods = mutableListOf<RangeObject.Range>()
                loadPeriods.add(RangeObject.Range(10, 1610521200))
                loadPeriods.add(RangeObject.Range(600, 1610522400))
                loadPeriods.add(RangeObject.Range(4200, 1610524500))
                loadPeriods.add(RangeObject.Range(2700, 1610538300))*/
                //заполняем периоды архива, которые есть на сервере
                availableRanges.clear()
                loadPeriods.forEach { period ->
                    //для теста
                    /*if (availableRanges.size == 0) {
                        period.from = 1602475470
                    }
                    period.from -= 3196800*/
                    availableRanges.add(
                        AvailableRange(
                            period.duration, period.from,
                            Instant.ofEpochSecond(period.from.toLong())
                                .atZone(ZoneId.systemDefault()).toLocalDateTime(),
                            Instant.ofEpochSecond(period.from.toLong() + period.duration.toLong())
                                .atZone(ZoneId.systemDefault()).toLocalDateTime()
                        )
                    )
                }

                val maxFrom =
                    loadPeriods.maxByOrNull { (it.from + it.duration) } ?: RangeObject.Range(0, 0)
                val minFrom = loadPeriods.minByOrNull { it.from }?.from ?: 0
                val maxDate = getDateTime((maxFrom.from + maxFrom.duration).toLong())
                val minDate = getDateTime(minFrom.toLong())
                val sdf = org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val localDateMax = LocalDate.parse(maxDate, sdf)
                val localDataMin = LocalDate.parse(minDate, sdf)
                if (boolean) {
                    closedRangeCalendar.value = Event(localDataMin.rangeTo(localDateMax))
                } else {
                    updateCalendar.value = Event(localDataMin.rangeTo(localDateMax))
                }
            }
        }
    }


    fun getCameras(model: VideoCameraModelP, onComplete: listenerEmpty) {
        viewModelScope.withProgress(progress = null) {
            cctvInteractor.getCCTV(model.houseId)?.let {
                state.set(cameraList_Key, it)
                state.set(cctvModel_Key, model)
                chooseCamera(0)
                onComplete()
            }
        }
    }


    //DataBase //TODO dataBase cache
    //////////////////////////////////////////////////
//    fun createItemDatabase(data: ImageItem) {
//        viewModelScope.launch(Dispatchers.IO) {
//            databaseCache.createItem(data)
//        }
//    }
//
//    fun getListDatabase() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val item = databaseCache.getImageItemById(15)
//                state.set(previewImage_key, listOf(item))
//            } catch (e: Exception) {
//            }
//        }
//    }
    ///////////////////////////////////////////////////

    fun chooseCamera(index: Int) {
        state.set(chosenIndex_Key, index)
        cameraList.value?.get(index)?.let { camera ->
            state.set(chosenId_Key, camera.id)
            state.set(chosenCamera_Key, camera)
//            downloadMaskImage(camera.preview)
        }
    }

    fun chooseCameraById(id: Int) {
        val camera = cameraList.value?.firstOrNull() { it.id == id }
        if (camera != null) {
            state.set(chosenId_Key, camera.id)
            state.set(chosenCamera_Key, camera)
            loadPeriod(false)
        }
    }

    fun setFavoriteCameraList(listId: List<Int>) {
        state.set(favoriteCamera_Key, listId)
        viewModelScope.launchSimple {
            cctvInteractor.setCCTVSort(listId)
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
        private const val chosenId_Key = "chosenId_Key"
        private const val chosenCamera_Key = "chosenCamera_Key"
        private const val favoriteCamera_Key = "favoriteCamera_Key"
        private const val stateFullScreen_Key = "stateFullScreen_Key"
        private const val cacheImage_key = "cacheImage_key"

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
