package com.sesameware.smartyard_oem

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.autofill.AutofillManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import org.linphone.core.TransportType
import org.osmdroid.util.GeoPoint
import org.threeten.bp.DayOfWeek
import org.threeten.bp.temporal.WeekFields
import com.sesameware.domain.model.FcmTransport
import com.sesameware.domain.model.response.CCTVCityCameraData
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.smartyard_oem.ui.map.LatLng
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@ColorInt
fun Resources.getColorCompat(resId: Int): Int {
    return ResourcesCompat.getColor(this, resId, null)
}

fun NavController.setStartDestination(id: Int) {
    val graph = this.graph
    graph.setStartDestination(id)
    this.graph = graph
}

fun FcmTransport.convert(): TransportType {
    return when (this) {
        FcmTransport.Udp -> TransportType.Udp
        FcmTransport.Tcp -> TransportType.Tcp
        FcmTransport.Tls -> TransportType.Tls
    }
}

fun View.show(on: Boolean, invisible: Boolean = false) {
    this.visibility = if (on) View.VISIBLE else if (invisible) View.INVISIBLE else View.GONE
}

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

fun dpToPx(dp: Int, context: Context): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
        context.resources.displayMetrics
    ).toInt()

internal fun Context.getDrawableCompat(@DrawableRes drawable: Int) =
    ContextCompat.getDrawable(this, drawable)

internal fun Context.getColorCompat(@ColorRes color: Int) = ContextCompat.getColor(this, color)

internal fun TextView.setTextColorRes(@ColorRes color: Int) =
    setTextColor(context.getColorCompat(color))

fun daysOfWeekFromLocale(): Array<DayOfWeek> {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    var daysOfWeek = DayOfWeek.values()
    // Order `daysOfWeek` array so that firstDayOfWeek is at index 0.
    if (firstDayOfWeek != DayOfWeek.MONDAY) {
        val rhs = daysOfWeek.sliceArray(firstDayOfWeek.ordinal..daysOfWeek.indices.last)
        val lhs = daysOfWeek.sliceArray(0 until firstDayOfWeek.ordinal)
        daysOfWeek = rhs + lhs
    }
    return daysOfWeek
}

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

fun String.isEmailCharacter(): Boolean {
    return this.contains("@")
}

fun hideKeyboard(activity: Activity) {
    val inputManager: InputMethodManager = activity
        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val currentFocusedView = activity.currentFocus
    if (currentFocusedView != null) {
        inputManager.hideSoftInputFromWindow(
            currentFocusedView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}

fun Int.reduceToZero(): Int {
    return if (this < 0) 0 else this
}
fun Double.clamp(min: Double, max: Double): Double {
    return max(min, min(max, this))
}
fun LatLng.toGeoPoint(): GeoPoint {
    return GeoPoint(latitude, longitude)
}
fun CCTVData.toLatLng(): LatLng {
    return LatLng(latitude ?: 0.0, longitude ?: 0.0)
}
fun CCTVCityCameraData.toLatLng(): LatLng {
    return LatLng(latitude ?: 0.0, longitude ?: 0.0)
}
fun List<LatLng>.getCenter(): LatLng {
    var lat: Double = 0.0
    var long: Double = 0.0
    forEach {
        lat += it.latitude
        long += it.longitude
    }
    return LatLng(lat / size, long / size)
}

fun String.removeTrailingZeros(): String {
    return BigDecimal(this).stripTrailingZeros().toPlainString()
}

@RequiresApi(Build.VERSION_CODES.M)
fun eventHandler(view: View, context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val afm = context.getSystemService(AutofillManager::class.java)
        afm?.requestAutofill(view)
    }
}
