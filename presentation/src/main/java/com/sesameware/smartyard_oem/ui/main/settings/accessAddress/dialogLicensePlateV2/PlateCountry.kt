package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogLicensePlateV2

import androidx.annotation.StringRes
import com.sesameware.smartyard_oem.R

/**
 * A country available for selection in the license plate dialog. Created exclusively from the
 * `listSupportedPlateNumberFormats` response, so it stores the country code and the corresponding
 * [regex].
 */
data class PlateCountry(
    val code: String,
    val regex: String
) {
    /** An emoji flag derived from the two-letter country code without a hardcoded country list. */
    val flagEmoji: String get() = PlateCountryCatalog.flagEmoji(code)

    /** The resource ID of the localized country name, or null if the country code is unknown. */
    @get:StringRes
    val displayNameRes: Int? get() = PlateCountryCatalog.displayNameRes(code)
}

/**
 * An extensible mapping of country codes to localized names and flags.
 * Unknown codes do not cause exceptions: the flag is derived from the code itself,
 * and the name is supplied by the calling code (usually the uppercase country code).
 */
object PlateCountryCatalog {
    private val displayNames: Map<String, Int> = mapOf(
        "ru" to R.string.country_ru,
        "am" to R.string.country_am,
        "by" to R.string.country_by,
        "kz" to R.string.country_kz
    )

    @StringRes
    fun displayNameRes(code: String): Int? = displayNames[code.lowercase()]

    /**
     * Builds an emoji flag from regional indicator symbols for any valid
     * two-letter ISO code. Returns a neutral white flag for an invalid code.
     */
    fun flagEmoji(code: String): String {
        val c = code.lowercase()
        if (c.length != 2 || c.any { it !in 'a'..'z' }) return "\uD83C\uDFF3\uFE0F"
        val base = 0x1F1E6
        val first = Character.toChars(base + (c[0] - 'a'))
        val second = Character.toChars(base + (c[1] - 'a'))
        return String(first) + String(second)
    }
}
