package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogLicensePlateV2

import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.Locale

/**
 * Calculates license plate validity periods and represents them in ISO 8601 format (timestamptz).
 *
 * All conversions use ThreeTen and take the device's time zone into account:
 * strings are formatted using [DateTimeFormatter.ISO_OFFSET_DATE_TIME] rather than string concatenation.
 * Automatic expiration is calculated by adding hours (not calendar days), correctly handling
 * transitions across midnight, month boundaries, and year boundaries.
 */
object PlateValidityCalculator {

    /** Combines a date and time into a [LocalDateTime]. */
    fun combine(date: LocalDate, time: LocalTime): LocalDateTime = LocalDateTime.of(date, time)

    /** Automatic expiration = start + [hours] hours. */
    fun autoEnd(start: LocalDateTime, hours: Int): LocalDateTime = start.plusHours(hours.toLong())

    /** The end must not be earlier than the start (equality is allowed). */
    fun isPeriodValid(start: LocalDateTime?, end: LocalDateTime?): Boolean {
        if (start == null || end == null) return true
        return !end.isBefore(start)
    }

    /**
     * Converts a local date and time to ISO 8601 with the device's time-zone offset.
     * For example: `2026-10-14T18:30:00+03:00`.
     */
    fun toIso(dateTime: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): String {
        val zoned = ZonedDateTime.of(dateTime, zone)
        return zoned.toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    /** Converts an ISO 8601 date-time string to a local date and time in the device's time zone. */
    fun parseIso(dateTime: String, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        OffsetDateTime
            .parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .atZoneSameInstant(zone)
            .toLocalDateTime()

    /**
     * Converts an ISO 8601 date-time string to a localized date-time string
     * using the device's locale and time zone.
     */
    fun fromIso(
        dateTime: String,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val zonedDateTime = OffsetDateTime
            .parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .atZoneSameInstant(zone)

        val formatter = DateTimeFormatter.ofLocalizedDateTime(
            FormatStyle.SHORT
        ).withLocale(locale)

        return zonedDateTime.format(formatter)
    }
}
