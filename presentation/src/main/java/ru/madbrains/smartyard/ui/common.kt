package ru.madbrains.smartyard.ui

import android.annotation.SuppressLint
import android.app.*
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BlendMode.SRC_ATOP
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff.Mode
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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.model.FcmCallData
import ru.madbrains.domain.utils.listenerEmpty
import ru.madbrains.domain.utils.listenerGeneric
import ru.madbrains.smartyard.LinphoneService
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.call.IncomingCallActivity
import ru.madbrains.smartyard.ui.call.IncomingCallActivity.Companion.FCM_DATA
import ru.madbrains.smartyard.ui.call.IncomingCallPushService
import ru.madbrains.smartyard.ui.widget.WidgetProvider
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun showStandardAlert(context: Context, @StringRes msgResId: Int, callback: listenerEmpty? = null) {
    showStandardAlert(context, context.getString(msgResId), callback)
}

fun showStandardAlert(
    context: Context,
    @StringRes titleResId: Int,
    @StringRes msgResId: Int,
    @StringRes buttonResId: Int,
    callback: listenerEmpty? = null,
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
    callback: listenerEmpty? = null,
) {
    AlertDialog.Builder(context, R.style.AlertDialogStyle)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
            callback?.run { this() }
        }
        .show()
}

fun showStandardAlert(
    context: Context,
    message: String,
    callback: listenerEmpty? = null,
    cancel: Boolean = true,
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
            progressBar.indeterminateDrawable, ContextCompat.getColor(context, R.color.blue_200)
        )

        frameLayout.addView(progressBar)
        frameLayout.visibility = if (hidden) View.GONE else View.VISIBLE
        return frameLayout
    }

    fun setColorFilter(drawable: Drawable, color: Int) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            drawable.colorFilter = BlendModeColorFilter(color, SRC_ATOP)
        } else {
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
    activity?.let {
        it.startActivity(
            Intent(
                ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }
}

enum class Type(var value: String) {
    INNER("inner"),
    OUTER("outer")
}

class SoundChooser {
    companion object {
        private const val RESULT_SOUND = 999
        fun getChosenTone(
            context: Context,
            type: Int,
            flatId: Int?,
            prefs: PreferenceStorage,
        ): RingtoneU {
            val path = getPath(type, flatId, prefs)
            val uri =
                if (path != null) Uri.parse(path) else RingtoneManager.getActualDefaultRingtoneUri(
                    context,
                    type
                )
            return RingtoneU(uri)
        }

        fun showSoundChooseIntent(
            fragment: Fragment,
            type: Int,
            flatId: Int?,
            prefs: PreferenceStorage,
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
                fragment.startActivityForResult(intent, RESULT_SOUND)
            }
        }

        fun getDataFromIntent(
            context: Context?,
            requestCode: Int,
            resultCode: Int,
            data: Intent?,
            callback: listenerGeneric<RingtoneU>,
        ) {
            if (requestCode == RESULT_SOUND && resultCode == RESULT_OK) {
                data?.let {
                    val uri = it.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
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

fun requestSoundChouser(context: Context): Boolean {
    if (!android.provider.Settings.System.canWrite(context)) {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:" + context.packageName)
        context.startActivity(intent)
    }
    return Settings.System.canWrite(context)
}

fun dpToPx(dp: Int): Float {
    return (dp * Resources.getSystem().getDisplayMetrics().density)
}

fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().getDisplayMetrics().density).toInt()
}


fun getStatusBarHeight(context: Context?, isNavBarHide: Boolean = false): Int {
    val resources: Resources = context?.resources ?: context?.applicationContext!!.resources
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        val statusBarHeight = resources.getDimensionPixelSize(resourceId)
        if (isNavBarHide) {
            statusBarHeight - (getBottomNavigationHeight(context) + dpToPx(10).toInt())
        } else {
            statusBarHeight
        }
    } else {
        0
    }
}

fun getBottomNavigationHeight(context: Context?): Int {
    return context?.resources?.let {
        val resourceId = it.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            it.getDimensionPixelSize(resourceId)
        } else 0
    } ?: 0
}


fun readExifData(imageFile: File? = null, name: String? = null): String? {
    return if (name != null) {
        val exif = ExifInterface(name)
        exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
    } else if (imageFile != null) {
        val exif = ExifInterface(imageFile.absolutePath)
        exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
    } else {
        null
    }
}

//@RequiresApi(VERSION_CODES.O)
//fun sendMessageReportTelegram(message: String) {
//    Timber.d("SENDSADASDAS $message")
//    val client = OkHttpClient()
//    val mediaType = "application/json; charset=utf-8".toMediaType()
//    val currentDataTime = LocalDateTime.now()
//    val formater = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
//    val time = currentDataTime.format(formater)
//    val json = JSONObject().apply {
//        put("chat_id", -672242342)
//        put("text", "<a href=\"#\">$time</a> <b>CENTRA.DOM</b>\n<pre>$message</pre>")
//        put("parse_mode", "HTML")
//    }
//    val body = json.toString().toRequestBody(mediaType)
//    val request = Request.Builder()
//        .url("https://api.telegram.org/bot5941499362:AAFeMAkL-7hAiIvHKuglo92_77ykiT4ENS8/sendMessage")
//        .post(body)
//        .build()
//    try {
//        client.newCall(request).execute().use { response ->
//            if (!response.isSuccessful) {
//                // Обработка ошибки
//                println("Unexpected code ${response.code}")
//            } else {
//                // Обработка успешного ответа
//                println("Message sent successfully")
//            }
//        }
//
//    } catch (_: Throwable) {
//    }
//}

@SuppressLint("PrivateResource")
fun sendCallNotification(
    data: FcmCallData,
    context: Context,
    prefs: PreferenceStorage,
) {
    context.run {
        val notId = prefs.notificationData.currentCallId
        val channelId = "CallNotificationUnlockScreen"
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            putExtra(FCM_DATA, data)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val intentOpenDoorCall = Intent(this, IncomingCallPushService::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = IncomingCallPushService.OPEN_DOOR
            putExtra(FCM_DATA, data)
        }
        val intentIgnoreCall = Intent(this, IncomingCallPushService::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = IncomingCallPushService.IGNORE_CALL
            putExtra(FCM_DATA, data)
        }

        val pendingIntentOpenDoorCall = PendingIntent.getService(
            this, 0, intentOpenDoorCall, if (VERSION.SDK_INT > VERSION_CODES.O_MR1) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val pendingIntentIgnoreCall = PendingIntent.getService(
            this, 0, intentIgnoreCall,
            if (VERSION.SDK_INT > VERSION_CODES.O_MR1) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setColor(ContextCompat.getColor(context, R.color.colorAccent))
            .setSound(soundUri)
            .setContentTitle(getString(R.string.call))
            .setContentText(getString(R.string.from, data.callerId))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter(30000)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setFullScreenIntent(pendingIntent, true)
            .addAction(R.drawable.exo_notification_small_icon, "Открыть", pendingIntentOpenDoorCall)
            .addAction(
                R.drawable.exo_notification_small_icon, "Игнорировать", pendingIntentIgnoreCall
            )
            .setShowWhen(true)

        Glide.with(this)
            .asBitmap()
            .load(data.image)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Timber.d("sendCallNotification onResourceReady start")
                    notificationBuilder.setStyle(
                        NotificationCompat.BigPictureStyle().bigPicture(resource)
                    )
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    if (notificationManager != null) {
                        if (VERSION.SDK_INT >= VERSION_CODES.O) {
                            val channel = NotificationChannel(
                                channelId,
                                "NotificationUnlockScreen",
                                NotificationManager.IMPORTANCE_HIGH
                            )
                            try {
                                LinphoneService.instance?.let { service ->
                                    ServiceCompat.startForeground(
                                        service, notId, notificationBuilder.build(),
                                        if (VERSION.SDK_INT >= VERSION_CODES.R) {
                                            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                                        } else {
                                            0
                                        }
                                    )
                                }
                            } catch (_: Exception) {
                            }
                            notificationManager.createNotificationChannel(channel)
                        } else {
                            notificationManager.notify(notId, notificationBuilder.build())
                        }
                    } else {
                        Timber.e("NotificationManager is null")
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)

                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    if (notificationManager != null) {
                        if (VERSION.SDK_INT >= VERSION_CODES.O) {
                            val channel = NotificationChannel(
                                channelId,
                                "NotificationUnlockScreen",
                                NotificationManager.IMPORTANCE_HIGH
                            )
                            notificationManager.createNotificationChannel(channel)
                            try {
                                LinphoneService.instance?.let { service ->
                                    ServiceCompat.startForeground(
                                        service, notId, notificationBuilder.build(),
                                        if (VERSION.SDK_INT >= VERSION_CODES.R) {
                                            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                                        } else {
                                            0
                                        }
                                    )
                                }
                            } catch (_: Exception) {
                            }
                        } else {
                            notificationManager.notify(notId, notificationBuilder.build())
                        }
                    } else {
                        Timber.e("NotificationManager is null")
                    }
                }
            })
    }
}


fun getChannelId(string: String): String {
    return CHANNEL_ID + string.replace(" ", "_")
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

fun requestPermissions(permissions: Array<String>, context: Context, requestCode: Int) {
    val permissionsToRequest = permissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
    if (permissionsToRequest.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            context as Activity,
            permissionsToRequest.toTypedArray(),
            requestCode
        )
    }
}

fun requestPermission(
    permissions: ArrayList<String>,
    context: Context,
    onGranted: listenerEmpty? = null,
    onDenied: listenerEmpty? = null,
) {
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
                token: PermissionToken?,
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
    text: String?,
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
            val customTypeface = ResourcesCompat.getFont(context, R.font.source_sans_pro_semi_bold)
            color = ResourcesCompat.getColor(context.resources, R.color.blue, null)
            isAntiAlias = true
            textSize = 40f
            typeface = customTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(it, canvas.width / 2f, canvas.height / 2f + 25f, textPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}


fun createIconWithoutText(
    context: Context,
    @DrawableRes bgRes: Int,
    @DrawableRes iconRes: Int,
): Drawable {
    val icon = ContextCompat.getDrawable(context, iconRes) as Drawable
    val bg = ContextCompat.getDrawable(context, bgRes) as Drawable
    val bitmap = Bitmap.createBitmap(bg.intrinsicWidth, bg.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    bg.setBounds(0, 0, canvas.width, canvas.height)
    bg.draw(canvas)
    icon.let {
        val left = (canvas.width - it.intrinsicWidth) / 2
        val right = (canvas.width + it.intrinsicWidth) / 2
        val top = (canvas.height - it.intrinsicHeight) / 2
        val bottom = (canvas.height + it.intrinsicHeight) / 2
        it.setBounds(left, top, right, bottom)
        it.draw(canvas)
    }

    return BitmapDrawable(context.resources, bitmap)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : Enum<T>> T.toInt(): Int = this.ordinal

private inline fun <reified T : Enum<T>> Int.toEnum(): T = enumValues<T>()[this]

private const val CHANNEL_ID = "smartyard_v6_"

//Анимация: fade in, затем fade out
fun animationFadeInFadeOut(view: View?) {
    view?.let {
        it.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(resources.getInteger(android.R.integer.config_longAnimTime).toLong())
                .withEndAction {
                    animate()
                        .alpha(0f)
                        .setDuration(
                            resources.getInteger(android.R.integer.config_longAnimTime).toLong()
                        )
                        .withEndAction {
                            visibility = View.INVISIBLE
                        }
                }
        }
    }
}

class DatePickerFragment(
    private val selectedDate: LocalDate,
    private val minDate: LocalDate? = null,
    private val callback: listenerGeneric<LocalDate>,
) : DialogFragment(), DatePickerDialog.OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = DatePickerDialog(
            requireContext(), this,
            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        if (minDate != null) {
            dialog.datePicker.minDate =
                ZonedDateTime.of(minDate, LocalTime.of(0, 0), ZoneId.systemDefault()).toInstant()
                    .toEpochMilli()
        }
        return dialog
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        callback(LocalDate.of(year, month + 1, dayOfMonth))
    }
}

class TimePickerFragment(
    private val selectedTime: LocalTime,
    private val callback: listenerGeneric<LocalTime>,
) : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return TimePickerDialog(
            requireContext(), this, selectedTime.hour, selectedTime.minute,
            DateFormat.is24HourFormat(requireContext())
        )
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        callback(LocalTime.of(hourOfDay, minute))
    }
}
