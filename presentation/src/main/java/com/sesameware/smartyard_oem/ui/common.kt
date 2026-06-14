package com.sesameware.smartyard_oem.ui

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_VIEW
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
import android.text.InputFilter
import android.text.Spanned
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.DatePicker
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sesameware.data.Crashlytics
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.model.PushCallData
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.MessagingService
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity.Companion.NOTIFICATION_ID
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity.Companion.PUSH_DATA
import com.sesameware.smartyard_oem.ui.show_event.ShowEventActivity
import com.sesameware.smartyard_oem.ui.widget.WidgetProvider
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.regex.PatternSyntaxException

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
            progressBar.indeterminateDrawable, ContextCompat.getColor(context, R.color.brand)
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

fun sendCallNotification(
    data: PushCallData,
    context: Context,
    prefs: PreferenceStorage
) {
    Timber.d("debug_dmm  call sendCallNotification")
    context.run {
        val notId = prefs.notificationData.currentCallId
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            putExtra(PUSH_DATA, data)
            putExtra(NOTIFICATION_ID, notId)
        }
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val notificationBuilder = NotificationCompat.Builder(this, MessagingService.CHANNEL_CALLS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setColor(ContextCompat.getColor(context, R.color.brand))
            .setContentTitle(getString(R.string.call))
            .setContentText(getString(R.string.from, data.callerId))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setTimeoutAfter(30_000)
            .setWhen(System.currentTimeMillis())
            .setFullScreenIntent(pendingIntent, true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = notificationBuilder.build()
        notification.flags += Notification.FLAG_INSISTENT
        notification.flags += Notification.FLAG_AUTO_CANCEL
        notificationManager.notify(notId, notification)
    }
}

fun sendEventNotification(
    title: String?,
    body: String?,
    date: String,
    imageUrl: String?,
    context: Context,
    prefs: PreferenceStorage) {
    Timber.d("debug_dmm  call sendEventNotification")

    prefs.notificationData.addInboxNotification(prefs)
    val notId = prefs.notificationData.currentInboxId

    context.run {
        val notifyIntent = Intent(this, ShowEventActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(ShowEventActivity.EVENT_TITLE, title)
            putExtra(ShowEventActivity.EVENT_BODY, body)
            putExtra(ShowEventActivity.EVENT_DATE, date)
            putExtra(ShowEventActivity.EVENT_IMAGE_URL, imageUrl)
        }
        val pendingIntent =
            PendingIntent.getActivity(this, notId, notifyIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        val notificationBuilder = NotificationCompat.Builder(this, MessagingService.CHANNEL_INBOX_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setColor(ContextCompat.getColor(context, R.color.brand))
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = notificationBuilder.build()
        notificationManager.notify(notId, notification)
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
    val bitmap = createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
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
            color = ResourcesCompat.getColor(context.resources, R.color.marker_tint, null)
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
    private val callback: listenerGeneric<LocalDate>
) : DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = DatePickerDialog(requireContext(), R.style.DatePickerStyle, this,
            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
        val serverMaxDate = LocalDateTime.now(ZoneId.of(timeZone))
        val localDateTime = LocalDateTime.now()
        val delta = serverMaxDate.toInstant(ZoneOffset.UTC).toEpochMilli() - localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        dialog.datePicker.maxDate = System.currentTimeMillis() + delta
        if (minDate != null) {
            dialog.datePicker.minDate = ZonedDateTime.of(minDate, LocalTime.of(0, 0), ZoneId.of(timeZone)).toInstant().toEpochMilli() + delta
        }
        dialog.window?.let {
            it.setBackgroundDrawableResource(R.drawable.background_dialog_large)
            val lp = it.attributes
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = lp
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

fun EditText.regexInputFilter(pattern: String) {
    try {
        filters = arrayOf<InputFilter>(
            RegexInputFilter(pattern)
        )
    } catch (e: PatternSyntaxException){
        isEnabled = false
        hint = e.message
    }
}

class RegexInputFilter(private var pattern: String) : InputFilter {
    private var maxLength: Int = Int.MAX_VALUE
    init {
        // find if exists the maximum length
        try {
            val res = Regex("^.*\\{(\\d*),*(\\d*)\\}.*$").find(pattern)?.groups
            if (res?.size == 3) {
                val l = res[1]?.value ?: ""
                val r = res[2]?.value ?: ""
                if (l.isNotEmpty() && r.isNotEmpty()) {
                    maxLength = if (r.isEmpty()) {
                        l.toInt()
                    } else {
                        r.toInt()
                    }
                }
            }
        } catch (_: Exception) {

        }

        // replace regexp pattern length range with * (if it exists)
        var start = -1
        var end = -1
        for (i in pattern.length - 1 downTo 0) {
            if (pattern[i] == '}') {
                end = i
            }
            if (pattern[i] == '{') {
                start = i
            }

            if (start >= 0 && end >= 0) {
                break
            }
        }
        if (start >= 0 && end >= 0) {
            pattern = StringBuilder(pattern.removeRange(start, end + 1)).apply {
                insert(start, "*")
            }.toString()

        }
        Timber.d("debug_dmm final regex pattern = $pattern")
    }

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        source?.let { s ->
            return if (Regex(pattern).matches(s) && dend < maxLength) {
                null
            } else {
                ""
            }
        }

        return null
    }
}

fun String.formatPhoneWith(pattern: String): String {
    val digits = this.filter { it.isDigit() }
    var i = 0
    return buildString {
        for (char in pattern) {
            if (i >= digits.length) break
            if (char == '#') {
                append(digits[i++])
            } else {
                append(char)
                if (char == digits[i]) {
                    i++
                }
            }
        }
    }
}

fun View.applyBottomNavInsetsToMargin() {
    val initialMarginBottom = marginBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bottomInset = insets
            .getInsets(WindowInsetsCompat.Type.navigationBars())
            .bottom

        val targetMargin = initialMarginBottom + bottomInset
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        if (layoutParams.bottomMargin != targetMargin) {
            layoutParams.updateMargins(bottom = targetMargin)
        }

        insets
    }
}

fun View.applyBottomNavInsetsToPadding() {
    val initialPaddingBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bottomInset = insets
            .getInsets(WindowInsetsCompat.Type.navigationBars())
            .bottom

        val targetPadding = initialPaddingBottom + bottomInset
        if (view.paddingBottom != targetPadding) {
            view.updatePadding(bottom = targetPadding)
        }

        insets
    }
}

fun String.toRegexOrNull() = takeIf { it.isNotBlank() }?.toRegex()

fun String.takeIfNotBlank() = takeIf { it.isNotBlank() }