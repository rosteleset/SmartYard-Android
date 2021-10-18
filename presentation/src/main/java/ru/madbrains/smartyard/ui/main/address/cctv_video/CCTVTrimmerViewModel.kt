package ru.madbrains.smartyard.ui.main.address.cctv_video

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Handler
import android.webkit.MimeTypeMap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import ru.madbrains.domain.interactors.CCTVInteractor
import ru.madbrains.domain.model.response.CCTVData
import ru.madbrains.domain.utils.listenerGeneric
import ru.madbrains.lib.HashBitmap
import ru.madbrains.lib.TimeInterval
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.clamp
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class CCTVTrimmerViewModel(
    private val cctvInteractor: CCTVInteractor
) : GenericViewModel() {
    private var mPlaybackState: Int = -1
    private var videoLoaderVisibility = false
    val videoLoaderVisible = MutableLiveData<Event<Boolean>>()
    val playState = MutableLiveData(false)
    val playSpeed = MutableLiveData(1.0)
    val uiMode = MutableLiveData(UiMode.Play)
    val alertEvent = MutableLiveData<Event<Int>>()
    val updateSeekTick = MutableLiveData<Event<Unit>>()
    val restoreSeek = MutableLiveData<Event<Long>>()
    private var progressTimer: Timer? = null

    val changePlayerInterval = MutableLiveData<Event<PlayerIntervalChangeData>>()
    val changeTrimInterval = MutableLiveData<Event<TrimmerIntervalChangeData>>()
    lateinit var mCurrentPlayerInterval: TimeInterval
    private var mCurrentSelection: TimeInterval? = null
    private val thumbDebounce = Debounce<Double>()
    private val videoFragmentDebounce = Debounce<TimeInterval>()
    private var thumbTask: Deferred<Unit?>? = null

    private var currentPositionInMs: Long = 0

    val trimmerPreviewImage = MutableLiveData<Bitmap?>()
    val playerMaskImages = MutableLiveData<Event<HashBitmap>>()
    val trimmerMaskImages = MutableLiveData<Event<HashBitmap>>()
    val shiftPickerPosition = MutableLiveData<Event<Long>>()

    val positionRecyclerViewInterval = MutableLiveData<Int>(0)

    //доступные интервалы архива для выбранной камеры
    private var availableRanges = mutableListOf<AvailableRange>()

    private lateinit var chosenCamera: CCTVData

    fun initialize(
        camera: CCTVData,
        initialThumb: Bitmap?
    ) {
        chosenCamera = camera
        initialThumb?.run {
            trimmerPreviewImage.postValue(this)
            playerMaskImages.postValue(Event(getBitmapList(this)))
            trimmerMaskImages.postValue(Event(getBitmapList(this)))
        }
    }

    private fun tryToDownload(context: Context) {
        mCurrentSelection?.run {
            val id = fragmentIdToDownload
            viewModelScope.withProgress {
                id?.let {
                    cctvInteractor.recDownload(it)?.let {
                        downloadFile(it, context)
                    } ?: run {
                        alertEvent.postValue(Event(dialogPrepareVideo))
                    }
                } ?: run {
                    cctvInteractor.recPrepare(chosenCamera.id, from, to)?.let { id ->
                        fragmentIdToDownload = id
                        alertEvent.postValue(Event(dialogPrepareVideo))
                    }
                }
            }
        }
    }

    fun savePlayerState(ms: Long) {
        this.currentPositionInMs = ms
    }

    private fun downloadFile(link: String, context: Context) {
        Timber.d("debug_dmm link: $link")
        val fileName = Regex(pattern = "[^/]+\$").find(link)
        fileName?.value?.let { name ->
            viewModelScope.withProgress(context = Dispatchers.IO) {
                val path = context.getExternalFilesDir(DIRECTORY_DOWNLOADS)
                URL(link).openStream().use { input ->
                    val file = File("$path/$name")
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                        val share = Intent(Intent.ACTION_SEND)
                        val extension =
                            Regex(pattern = "\\.([0-9a-z]+)\$").find(name)?.groupValues?.get(1)
                        if (extension != null) {
                            share.type =
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                        }
                        share.putExtra(
                            Intent.EXTRA_STREAM,
                            Uri.parse(file.path)
                        )
                        context.startActivity(
                            Intent.createChooser(
                                share,
                                ""
                            )
                        )
                    }
                }
            }
        }
    }

    fun toggleVideoPlay() {
        val current = playState.value ?: false
        play(!current)
    }

    fun stopVideoPlay() {
        play(false)
    }

    fun changeSpeed(offset: Double) {
        val value = (playSpeed.value ?: 1.0) * offset
        playSpeed.postValue(value.clamp(0.125, 32.0))
    }

    fun restoreSeek() {
        restoreSeek.postValue(Event(currentPositionInMs))
    }

    private fun normalSpeed() {
        playSpeed.postValue(1.0)
    }

    fun showVideoLoader(visible: Boolean) {
        videoLoaderVisibility = visible
        if (uiMode.value == UiMode.Play) {
            videoLoaderVisible.postValue(Event(visible))
        }
    }

    fun changeUIMode(mode: UiMode) {
        uiMode.postValue(mode)
        videoLoaderVisible.postValue(Event(mode == UiMode.Play && videoLoaderVisibility))
        stopVideoPlay()
        if (mode == UiMode.Trim && ::mCurrentPlayerInterval.isInitialized) {
            val centerTime = mCurrentPlayerInterval.from.plusSeconds(currentPositionInMs / 1000)
            //val newInterval = mCurrentPlayerInterval.getTrimInterval(centerTime)
            val newInterval = getTrimInterval(centerTime)
            changeTrimmerInterval(newInterval, true)
        }
    }

    fun pressMainButton(context: Context) {
        if (uiMode.value == UiMode.Trim) {
            tryToDownload(context)
        } else {
            changeUIMode(UiMode.Trim)
        }
    }

    private fun play(boolean: Boolean) {
        playState.postValue(boolean)
        startListeningProgress(boolean)
    }

    fun changePlaybackState(playbackState: Int) {
        if (playbackState != mPlaybackState) {
            when (playbackState) {
                Player.STATE_READY -> {
                    showVideoLoader(false)
                    startListeningProgress(playState.value ?: false)
                }
                Player.STATE_BUFFERING -> {
                    showVideoLoader(true)
                    normalSpeed()
                }
                Player.STATE_ENDED -> {
                    //игнорируем, так как обрабатывается в CCTVTimmerFragment
                }
                else -> {
                    stopVideoPlay()
                    showVideoLoader(false)
                }
            }
        }
        mPlaybackState = playbackState
    }

    private fun startListeningProgress(play: Boolean) {
        if (play && progressTimer == null) {
            progressTimer = fixedRateTimer("timer", false, 0, 400) {
                updateSeekTick.postValue(Event(Unit))
            }
        } else if (!play) {
            clearProgressTimer()
        }
    }

    fun positionItemRecyclerViewInterval(positionItem: Int) {
        positionRecyclerViewInterval.value = positionItem
    }

    fun changePlayerInterval(interval: TimeInterval) {
        mCurrentPlayerInterval = interval
        startListeningProgress(false)
        val hls = chosenCamera.getHlsAt(interval.from, interval.durationSeconds)
        changePlayerInterval.postValue(Event(PlayerIntervalChangeData(interval, hls)))
        videoFragmentDebounce.doBlocking(interval) {
            downloadMaskImages(interval, false)
            downloadThumbAt(0.0, interval)
        }
    }
    private fun changeTrimmerInterval(interval: TimeInterval, reset: Boolean) {
        changeTrimInterval.postValue(Event(TrimmerIntervalChangeData(interval, reset)))
        videoFragmentDebounce.doBlocking(interval) {
            downloadMaskImages(interval, true)
            downloadThumbAt(0.0, interval)
        }
    }

    private fun checkLimit(interval: TimeInterval): Boolean {
        if (availableRanges.size == 0) {
            return false
        }

        return interval.from >= availableRanges[0].startDate && interval.to <= availableRanges.last().endDate
    }

    fun changeTrimmerIntervalTo(seconds: Int, interval: TimeInterval) {
        val newInterval = interval.offsetInterval(seconds.toLong())
        if (checkLimit(newInterval)) {
            changeTrimmerInterval(newInterval, false)
        } else {
            shiftPickerPosition.postValue(Event(seconds * 1000L))
        }
    }

    fun downloadThumbAt(per: Double, interval: TimeInterval) {
        thumbDebounce.doBlocking(per) {
            downloadThumbAtNow(it, interval)
        }
    }

    private fun downloadThumbAtNow(percent: Double, interval: TimeInterval) {
        thumbTask?.cancel()
        thumbTask = viewModelScope.async(Dispatchers.IO) {
            val url = chosenCamera.getPreviewAt(interval.getTimeAtProgress(percent))
            trimmerPreviewImage.postValue(downloadPreview(url) ?: Bitmap.createBitmap(0, 0, Bitmap.Config.ARGB_8888))
        }
    }

    private fun getBitmapList(bitmap: Bitmap): HashBitmap {
        val hash = HashBitmap()
        (0 until 5).forEach {
            hash[it] = bitmap
        }
        return hash
    }

    private fun downloadMaskImages(time: TimeInterval, trimMode: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val tasks = mutableListOf<Deferred<Unit>>()
            val range = getThumbnailRange(time, 5)
            val hash = HashBitmap()
            range.forEachIndexed { index, it ->
                val url = chosenCamera.getPreviewAt(it)
                val task = viewModelScope.async(Dispatchers.IO) {
                    hash[index] = downloadPreview(url)
                }
                tasks.add(task)
            }
            tasks.awaitAll().let {
                if (trimMode) {
                    trimmerMaskImages.postValue(Event(hash))
                } else {
                    playerMaskImages.postValue(Event(hash))
                }
            }
        }
    }

    private fun getThumbnailRange(time: TimeInterval, num: Int): ArrayList<LocalDateTime> {
        val arr = arrayListOf<LocalDateTime>()
        (0 until num).forEach {
            val new = time.getTimeAtProgress(it.toDouble() / num)
            arr.add(new)
        }
        return arr
    }

    private fun downloadPreview(url: String): Bitmap? {
        return try {
            CCTVViewModel.downloadPreview(url)
        } catch (e: Throwable) {
            handleError(e)
            null
        }
    }

    fun handleError(e: Throwable) {
        Timber.d("debug_dmm e: $e")
    }

    data class PlayerIntervalChangeData(
        val interval: TimeInterval,
        val hls: String
    )
    data class TrimmerIntervalChangeData(
        val interval: TimeInterval,
        val reset: Boolean
    )

    class Debounce<T> {
        private var data: T? = null
        private val handler: Handler = Handler()
        private var block: Boolean = false
        private var pending: Boolean = false
        fun doBlocking(data: T, listenerGeneric: listenerGeneric<T>) {
            this.data = data
            if (!block) {
                listenerGeneric(data)
                scheduleTask(listenerGeneric)
                block = true
            } else {
                pending = true
            }
        }

        private fun scheduleTask(listenerGeneric: listenerGeneric<T>) {
            handler.postDelayed(
                {
                    block = false
                    if (pending) {
                        pending = false
                        data?.let { listenerGeneric(it) }
                    }
                },
                500
            )
        }
    }

    private fun clearProgressTimer() {
        progressTimer?.cancel()
        progressTimer = null
    }

    override fun onCleared() {
        super.onCleared()
        clearProgressTimer()
    }

    fun saveCurrentSelection(timeInterval: TimeInterval) {
        Timber.d("debug_dmm setSelect")
        mCurrentSelection = timeInterval
    }
    
    fun setAvailableRanges(ranges: List<AvailableRange>) {
        availableRanges = ranges.toMutableList()
    }

    private fun getTrimInterval(centerTime: LocalDateTime): TimeInterval {
        var newFrom = centerTime.plusSeconds(-TimeInterval.TIME_OFFSET)
        var newTo = centerTime.plusSeconds(TimeInterval.TIME_OFFSET)
        if (availableRanges.size > 0) {
            if (newFrom < availableRanges[0].startDate) {
                newFrom = availableRanges[0].startDate
                newTo = newFrom.plusSeconds(TimeInterval.TIME_OFFSET * 2)
            } else if (newTo > availableRanges.last().endDate) {
                newTo = availableRanges.last().endDate
                newFrom = newTo.plusSeconds(-TimeInterval.TIME_OFFSET * 2)
            }
        }

        return TimeInterval(newFrom, newTo)
    }

    companion object {
        const val dialogPrepareVideo: Int = 1
        const val SEEK_STEP = 15_000L  // интервал перемотки в миллисекундах видео архива при двойном тапе
    }

    enum class UiMode {
        Trim, Play
    }
}
