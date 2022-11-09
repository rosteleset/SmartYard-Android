package com.sesameware.smartyard_oem.ui

import android.app.*
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.res.Resources
import android.graphics.*
import android.graphics.BlendMode.SRC_ATOP
import android.graphics.PorterDuff.Mode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.model.FcmCallData
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.FirebaseMessagingService
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity.Companion.FCM_DATA
import com.sesameware.smartyard_oem.ui.widget.WidgetProvider
import okhttp3.OkHttpClient
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.io.InputStream
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
    callback: listenerEmpty? = null
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
    activity?.startActivity(
        Intent(
            ACTION_VIEW,
            Uri.parse(url)
        )
    )
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
            prefs: PreferenceStorage
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

fun dpToPx(dp: Int): Float {
    return (dp * Resources.getSystem().getDisplayMetrics().density)
}

fun getBottomNavigationHeight(context: Context?): Int {
    return context?.resources?.let {
        val resourceId = it.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            it.getDimensionPixelSize(resourceId)
        } else 0
    } ?: 0
}

fun sendCallNotification(
    data: FcmCallData,
    context: Context,
    prefs: PreferenceStorage
) {
    context.run {
        val notId = prefs.notificationData.currentCallId
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            putExtra(FCM_DATA, data)
        }
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent,
                if (VERSION.SDK_INT >= VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, FirebaseMessagingService.CHANNEL_CALLS_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setColor(ContextCompat.getColor(context, R.color.colorAccent))
            .setContentTitle(getString(R.string.call))
            .setContentText(getString(R.string.from, data.callerId))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)  //отключаем звук уведомления, так как он запускается при успешном sip соединении
            .setVibrate(FirebaseMessagingService.CALL_VIBRATION_PATTERN)  //задаём шаблон вибрации
            .setTimeoutAfter(30000)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setFullScreenIntent(pendingIntent, true)
            .setShowWhen(true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = notificationBuilder.build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT  //зацикленная вибрация и звук при уведомлении
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
                        .setDuration(resources.getInteger(android.R.integer.config_longAnimTime).toLong())
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
    private val callback: listenerGeneric<LocalDate>) : DialogFragment(), DatePickerDialog.OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = DatePickerDialog(requireContext(), this,
            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
        dialog.datePicker.maxDate = System.currentTimeMillis()
        if (minDate != null) {
            dialog.datePicker.minDate = ZonedDateTime.of(minDate, LocalTime.of(0, 0), ZoneId.systemDefault()).toInstant().toEpochMilli()
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

@GlideModule
class IgnoreSSLErrorsGlideModule : AppGlideModule() {

    private fun createHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory
        val builder = OkHttpClient.Builder()
        with(builder) {
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
            sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager).hostnameVerifier{ _, _ -> true}
        }
        return builder.build()
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val okHttpClient = createHttpClient()
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient))
    }
}
