package ru.madbrains.smartyard.ui.player.classes

import android.app.DatePickerDialog
import android.content.Context
import android.icu.util.Calendar
import ru.madbrains.domain.utils.listenerEmpty
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DatePicker(
    private val context: Context,
    private val mExoPlayerViewModel: ExoPlayerViewModel
) {
    fun createDatePickerDialog(
        pastDateSplit: Long,
        presentDateSplit: Long,
        onComplete: listenerEmpty
    ): DatePickerDialog? {
        return try {
            // Получение текущей даты
            val calendarTime = mExoPlayerViewModel.calendar.value ?: return null
            val calendar = Calendar.getInstance()
            calendar.time = Date(calendarTime)
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            // Создание и отображение DatePickerDialog
            val datePickerDialog = DatePickerDialog(
                context,
                { view, yea, mon, dayOf ->
                    // Обработка выбранной даты
                    val currentSelectedDate = Calendar.getInstance()
                    currentSelectedDate.set(yea, mon, dayOf)
                    mExoPlayerViewModel.setCalendar(currentSelectedDate.timeInMillis)
                    onComplete()
                },
                year, month, dayOfMonth
            )
            val minDate = Calendar.getInstance()
            val minCalendarDate = Calendar.getInstance()
            minCalendarDate.time = Date(pastDateSplit)
            minDate.set(
                minCalendarDate.get(Calendar.YEAR),
                minCalendarDate.get(Calendar.MONTH),
                minCalendarDate.get(Calendar.DAY_OF_MONTH)
            )
            val maxDate = Calendar.getInstance()
            val maxCalendarDate = Calendar.getInstance()
            maxCalendarDate.time = Date(presentDateSplit)
            maxDate.set(
                maxCalendarDate.get(Calendar.YEAR),
                maxCalendarDate.get(Calendar.MONTH),
                maxCalendarDate.get(Calendar.DAY_OF_MONTH)
            )
            val datePicker = datePickerDialog.datePicker
            datePicker.minDate = minDate.timeInMillis
            datePicker.maxDate = maxDate.timeInMillis

            datePickerDialog
        } catch (e: Exception) {
            Timber.e(e, "Error in DatePicker creation")
            null
        }
    }
}