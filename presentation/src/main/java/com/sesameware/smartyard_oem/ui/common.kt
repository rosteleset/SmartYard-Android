package com.sesameware.smartyard_oem.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BlendMode.SRC_ATOP
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff.Mode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.model.PushCallData
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.Crashlytics
import com.sesameware.smartyard_oem.LinphoneService
import com.sesameware.smartyard_oem.MessagingService
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity.Companion.PUSH_DATA
import com.sesameware.smartyard_oem.ui.widget.WidgetProvider
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

fun showStandardAlert(context: Context, @StringRes msgResId: Int, callback: listenerEmpty? = null) {
    showStandardAlert(context, context.getString(msgResId), callback)
}
fun showStandardAlert(
    context: Context,
    @StringRes titleResId: Int,
    @StringRes msgResId: Int,
    @StringRes buttonResId: Int,
    callback: listenerEmpty? = null
) {
    context.run {
        AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle(getString(titleResId))
            .setMessage(getString(msgResId))
            .setCancelable(true)
            .setPositiveButton(buttonResId) { _: DialogInterface?, _: Int ->
                callback?.run { this() }
            }
            .show()
    }
}

fun showStandardAlert(
    context: Context,
    title: String?,
    message: String,
    cancel: Boolean = true,
    callback: listenerEmpty? = null
) {
    AlertDialog.Builder(context, R.style.AlertDialogStyle)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(cancel)
        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
            callback?.run { this() }
        }
        .show()
}

fun showStandardAlert(
    context: Context,
    message: String,
    callback: listenerEmpty? = null,
    cancel: Boolean = true
) {
    AlertDialog.Builder(context, R.style.AlertDialogStyle)
        .setMessage(message)
        .setCancelable(cancel)
        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
            callback?.run { this() }
        }
        .show()
}

class ProgressDialog {
    private var mDialog: Dialog? = null
    private val isShowing: Boolean get() = mDialog?.isShowing ?: false
    fun getView(context: Context, hidden: Boolean = false): FrameLayout {
        val progressBar = ProgressBar(context)
        val frameLayout = FrameLayout(context)
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        frameLayout.layoutParams = layoutParams
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER

        progressBar.layoutParams = lp

        setColorFilter(
            progressBar.indeterminateDrawable, ContextCompat.getColor(context, R.color.colorAccent)
        )

        frameLayout.addView(progressBar)
        frameLayout.visibility = if (hidden) View.GONE else View.VISIBLE
        return frameLayout
    }

    fun setColorFilter(drawable: Drawable, color: Int) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            drawable.colorFilter = BlendModeColorFilter(color, SRC_ATOP)
        } else {
            @Suppress("DEPRECATION")
            drawable.setColorFilter(color, Mode.SRC_ATOP)
        }
    }

    fun createDialog(context: Context?): ProgressDialog {
        context?.let {
            val dialog = Dialog(it)
            dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)

            val view = getView(it)
            dialog.window?.setContentView(view, view.layoutParams)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            mDialog = dialog
            mDialog?.setCanceledOnTouchOutside(true)
        }
        return this
    }

    fun showDialog() {
        if (!isShowing) mDialog?.show()
    }

    fun dismissDialog() {
        if (isShowing) mDialog?.dismiss()
    }
}

fun firstCharacter(text: String?): String {
    return if (text != null) {
        if (text.isNotEmpty()) text.first() + "." else ""
    } else ""
}

fun openUrl(activity: Activity?, url: String) {
    activity?.startActivity(
        Intent(
            ACTION_VIEW,
            Uri.parse(url)
        )
    )
}

enum class Type(var value: String) {
    INNER("inner"),
    OUTER("outer"),
    OWNER("owner")
}

class SoundChooser {
    companion object {
        private const val RESULT_SOUND = 999
        fun getChosenTone(
            context: Context,
            type: Int,
            flatId: Int?,
            prefs: PreferenceStorage
        ): RingtoneU {
            val path = getPath(type, flatId, prefs)
            try {
                val uri = if (path != null) {
                    Uri.parse(path)
                } else {
                    if (type == RingtoneManager.TYPE_RINGTONE) {
                        Settings.System.DEFAULT_RINGTONE_URI
                    } else {
                        Settings.System.DEFAULT_NOTIFICATION_URI
                    }
                }
                return RingtoneU(uri)
            } catch (e: Throwable) {
                val crashlytics = Crashlytics.getInstance()
                crashlytics.setCustomKey("_melody_data", "flat_id = $flatId, path = ${path.orEmpty()}")
                crashlytics.recordException(e)
                crashlytics.setCustomKey("_melody_data", "")
            }

            return RingtoneU(null)
        }

        fun showSoundChooseIntent(
            fragment: Fragment,
            type: Int,
            flatId: Int?,
            prefs: PreferenceStorage
        ) {
            fragment.context?.let { context ->
                val currentTone = getChosenTone(context, type, flatId, prefs)
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type)
                intent.putExtra(
                    RingtoneManager.EXTRA_RINGTONE_TITLE,
                    context.getString(R.string.choose_sound)
                )
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone.uri)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                @Suppress("DEPRECATION")
                fragment.startActivityForResult(intent, RESULT_SOUND)
            }
        }

        fun getDataFromIntent(
            context: Context?,
            requestCode: Int,
            resultCode: Int,
            data: Intent?,
            callback: listenerGeneric<RingtoneU>
        ) {
            if (requestCode == RESULT_SOUND && resultCode == RESULT_OK) {
                data?.let {
                    @Suppress("DEPRECATION") val uri = it.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    uri?.let {
                        context?.let {
                            callback(RingtoneU(uri))
                        }
                    }
                }
            }
        }

        private fun getPath(type: Int, flatId: Int?, prefs: PreferenceStorage): String? {
            return when {
                type == RingtoneManager.TYPE_NOTIFICATION -> prefs.notifySoundUri
                flatId != null -> prefs.addressOptions.getOption(flatId).notifySoundUri
                else -> null
            }
        }
    }

    data class RingtoneU(val uri: Uri?) {
        fun getToneTitle(context: Context): String {
            return RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: ""
        }
    }
}

fun Context.dimenToPx(@DimenRes id: Int): Int = resources.getDimensionPixelSize(id)

fun dpToPx(dp: Int): Float {
    return (dp * Resources.getSystem().displayMetrics.density)
}

@SuppressLint("InternalInsetResource")
fun getBottomNavigationHeight(context: Context?): Int {
    return context?.resources?.let {
        val resourceId = it.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            it.getDimensionPixelSize(resourceId)
        } else 0
    } ?: 0
}

@SuppressLint("ObsoleteSdkInt")
fun sendCallNotification(
    data: PushCallData,
    context: Context,
    prefs: PreferenceStorage
) {
    context.run {
        val notId = prefs.notificationData.currentCallId
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            putExtra(PUSH_DATA, data)
        }
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent,
                if (VERSION.SDK_INT >= VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, MessagingService.CHANNEL_CALLS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setColor(ContextCompat.getColor(context, R.color.colorAccent))
            .setContentTitle(getString(R.string.call))
            .setContentText(getString(R.string.from, data.callerId))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)  //отключаем звук уведомления, так как он запускается при успешном sip соединении
            .setVibrate(null)  //отключаем вибрацию в уведомлении, так как она запускается при успешном sip соединении
            .setTimeoutAfter(30000)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setFullScreenIntent(pendingIntent, true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = notificationBuilder.build()
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            try {
                LinphoneService.instance?.let { service ->
                    ServiceCompat.startForeground(service, notId, notification,
                        if (VERSION.SDK_INT >= VERSION_CODES.R) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                        } else {
                            0
                        })
                }
            } catch (e: Exception) {
                val crashlytics = Crashlytics.getInstance()
                crashlytics.recordException(e)
            }
        } else {
            notificationManager.notify(notId, notification)
        }
    }
}

fun updateAllWidget(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(
        ComponentName(
            context,
            WidgetProvider::class.java
        )
    )
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.lvList)
    Timber.tag(
        "Widget"
    ).d(
        "Update all widget"
    )
}

fun requestPermission(permissions: ArrayList<String>, context: Context, onGranted: listenerEmpty? = null, onDenied: listenerEmpty? = null) {
    Dexter.withContext(context)
        .withPermissions(permissions)
        .withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report?.areAllPermissionsGranted() == true) {
                    onGranted?.invoke()
                } else {
                    onDenied?.invoke()
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                request: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                token?.continuePermissionRequest()
            }
        }).check()
}

fun resourceToBitmap(context: Context, drawableSrc: Int): Bitmap {
    val drawable = ResourcesCompat.getDrawable(context.resources, drawableSrc, null) as Drawable
    return drawableToBitmap(drawable)
}

private fun drawableToBitmap(vectorDrawable: Drawable): Bitmap {
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    vectorDrawable.draw(canvas)
    return bitmap
}
fun createIconWithText(
    context: Context,
    @DrawableRes bgRes: Int,
    @DrawableRes iconRes: Int,
    text: String?
): Drawable {
    val icon = ContextCompat.getDrawable(context, iconRes) as Drawable
    val bg = ContextCompat.getDrawable(context, bgRes) as Drawable
    val bitmap = Bitmap.createBitmap(bg.intrinsicWidth, bg.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    bg.setBounds(0, 0, canvas.width, canvas.height)
    bg.draw(canvas)
    icon.let {
        val offset = 30
        val left = (canvas.width - it.intrinsicWidth) / 2
        val right = (canvas.width + it.intrinsicWidth) / 2
        val top = (canvas.height - it.intrinsicHeight) / 2 - offset
        val bottom = (canvas.height + it.intrinsicHeight) / 2 - offset
        it.setBounds(left, top, right, bottom)
        it.draw(canvas)
    }

    text?.let {
        val textPaint: Paint = Paint().apply {
            color = ResourcesCompat.getColor(context.resources, R.color.blue, null)
            isAntiAlias = true
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(it, canvas.width / 2f, canvas.height / 2f + 25f, textPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : Enum<T>> T.toInt(): Int = this.ordinal

private inline fun <reified T : Enum<T>> Int.toEnum(): T = enumValues<T>()[this]

private const val CHANNEL_ID = "smartyard_v6_"

//Анимация: fade in, затем fade out
fun animationFadeInFadeOut(view: View?) {
    view?.apply {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(resources.getInteger(android.R.integer.config_longAnimTime).toLong())
            .withEndAction {
                animate()
                    .alpha(0f)
                    .setDuration(resources.getInteger(android.R.integer.config_longAnimTime).toLong())
                    .withEndAction {
                        visibility = View.INVISIBLE
                    }
            }
    }
}

class DatePickerFragment(
    private val selectedDate: LocalDate,
    private val timeZone: String = DataModule.serverTz,
    private val minDate: LocalDate? = null,
    private val callback: listenerGeneric<LocalDate>) : DialogFragment(), DatePickerDialog.OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = DatePickerDialog(requireContext(), this,
            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
        val serverMaxDate = LocalDateTime.now(ZoneId.of(timeZone))
        val localDateTime = LocalDateTime.now()
        val delta = serverMaxDate.toInstant(ZoneOffset.UTC).toEpochMilli() - localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        dialog.datePicker.maxDate = System.currentTimeMillis() + delta
        if (minDate != null) {
            dialog.datePicker.minDate = ZonedDateTime.of(minDate, LocalTime.of(0, 0), ZoneId.of(timeZone)).toInstant().toEpochMilli() + delta
        }
        return dialog
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        callback(LocalDate.of(year, month + 1, dayOfMonth))
    }
}

class TimePickerFragment(
    private val selectedTime: LocalTime,
    private val callback: listenerGeneric<LocalTime>) : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return TimePickerDialog(requireContext(), this, selectedTime.hour, selectedTime.minute,
            DateFormat.is24HourFormat(requireContext()))
    }
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        callback(LocalTime.of(hourOfDay, minute))
    }
}

fun Fragment.getStatusBarHeight(): Int {
    val rect = Rect()
    requireActivity().window.decorView.getWindowVisibleDisplayFrame(rect)
    return rect.top
}

fun Context.toast(@StringRes resId: Int, long: Boolean = true) {
    val length = if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    Toast.makeText(this, getString(resId), length).show()
}

fun Context.toast(message: String, long: Boolean = true) {
    val length = if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    Toast.makeText(this, message, length).show()
}
