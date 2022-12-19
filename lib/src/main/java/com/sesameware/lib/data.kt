package com.sesameware.lib

import android.graphics.Bitmap
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

typealias HashBitmap = HashMap<Int, Bitmap?>
typealias SeekListener = (Double) -> Unit
typealias SelectionListener = (LocalDateTime, LocalDateTime) -> Unit
data class TimeInterval(
    val from: LocalDateTime,
    val to: LocalDateTime
) {
    var fragmentIdToDownload: Int? = null
    val intervalText: String get() = "${from.format(mIntervalFormatter)} - ${to.format(mIntervalFormatter)}"
    val durationSeconds: Long get() = durationInMs / 1000
    val durationInMs: Long get() = to.timeInMs() - from.timeInMs()

    private fun getPosInMs(current: LocalDateTime): Long {
        return (current.timeInMs() - from.timeInMs()).coerceIn(0L, durationInMs)
    }

    fun getProgressWith(time: LocalDateTime): Double {
        return getPosInMs(time).toDouble() / durationInMs
    }

    fun getTimeAtProgress(percent: Double): LocalDateTime {
        return from.plusSeconds((percent * durationSeconds).toLong())
    }

    fun offsetInterval(offsetSeconds: Long): TimeInterval {
        return TimeInterval(from.plusSeconds(offsetSeconds), to.plusSeconds(offsetSeconds))
    }

    fun getTrimInterval(centerTime: LocalDateTime): TimeInterval {
        val newFrom = centerTime.plusSeconds(-TIME_OFFSET)
        val newTo = centerTime.plusSeconds(TIME_OFFSET)
        /*if (newFrom.isBefore(startLimit)) {
            return getTrimInterval(startLimit.plusSeconds(TIME_OFFSET))
        }
        if (newTo.isAfter(endLimit)) {
            return getTrimInterval(endLimit.plusSeconds(-TIME_OFFSET))
        }*/
        return TimeInterval(newFrom, newTo/*, startLimit, endLimit*/)
    }

    companion object {
        const val INTERVAL_STEP_ = 3L
        const val TIME_OFFSET = 30L * 60
        private val mIntervalFormatter = DateTimeFormatter.ofPattern("HH.mm")
    }
}
