package ru.madbrains.smartyard.ui.player.classes

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.marginTop
import ru.madbrains.smartyard.databinding.FragmentExoPlayerBinding
import ru.madbrains.smartyard.ui.player.ExoPlayerFragment
import java.math.BigDecimal
import java.text.SimpleDateFormat

class TimerWatcher(val binding: FragmentExoPlayerBinding) {
    @SuppressLint("SimpleDateFormat")
    fun getTime(firstVisibleView: View, rangeTime: Long, scale: Int = ExoPlayerFragment.SCALE_X1): Long {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        val timerMarginTop = BigDecimal(binding.clTimer.marginTop)
        val timerHeightHalf = BigDecimal(binding.clTimer.height.toDouble() / 2)
        val timerHeightLine = timerHeightHalf.plus(timerMarginTop)

        val firstElementY =
            (BigDecimal(firstVisibleView.y.toDouble()) - timerMarginTop - timerHeightHalf)
        val heightElement = firstVisibleView.height
        val timeCorrection = BigDecimal(scale * 60).div(BigDecimal(4))
        var timeInSecond = rangeTime
        val timeInLine = run {
            timeInSecond /= 1000
            val timeInSeconds = BigDecimal(timeInSecond)
            val heightInOneSecond =
                BigDecimal(heightElement.toDouble() / (scale * 60))
            val xSeconds = firstElementY / heightInOneSecond
            val currentPositionInSeconds = timeInSeconds.plus(xSeconds)
            currentPositionInSeconds.multiply(BigDecimal(1000)).toLong()
    //            val millisecondsSinceEpoch = currentPositionInSeconds.multiply(BigDecimal("1000"))
    //            timeFormat.format(Date(millisecondsSinceEpoch.toLong()))
        }
        return timeInLine
    }

}