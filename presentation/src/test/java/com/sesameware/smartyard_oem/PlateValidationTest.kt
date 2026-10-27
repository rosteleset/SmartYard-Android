package com.sesameware.smartyard_oem

import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogLicensePlateV2.PlateCountryCatalog
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogLicensePlateV2.PlateNumberValidator
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogLicensePlateV2.PlateValidityCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset

class PlateValidationTest {
    // Реальный формат российского номера, используемый в проекте.
    private val ruRegex = "[АВЕКМНОРСТУХ][0-9]{3}[АВЕКМНОРСТУХ]{2}[0-9]{2,3}"
    // Иной формат для проверки повторной валидации при смене страны.
    private val digitsRegex = "[0-9]{4}[АВЕКМНОРСТУХ]{2}[0-9]{2,3}"

    @Test fun completeNumberAllowed() {
        val validator = PlateNumberValidator(ruRegex)
        assertTrue(validator.isComplete("А123АА77"))
        assertTrue(validator.isComplete("А123АА777"))
    }

    @Test fun incompletePrefixAllowedButNotComplete() {
        val validator = PlateNumberValidator(ruRegex)
        assertTrue(validator.isValidPrefix("А"))
        assertTrue(validator.isValidPrefix("А123АА"))
        assertFalse(validator.isComplete("А123АА"))
    }

    @Test fun disallowedCharactersRejectedAsPrefix() {
        val validator = PlateNumberValidator(ruRegex)
        // Первый символ должен быть буквой из допустимого набора, а не цифрой.
        assertFalse(validator.isValidPrefix("1"))
        // Буква не из набора.
        assertFalse(validator.isValidPrefix("Б"))
    }

    @Test fun exceedingMaxLengthRejected() {
        val validator = PlateNumberValidator(ruRegex)
        // 4 цифры региона превышают максимум из 3 — больше не префикс.
        assertFalse(validator.isValidPrefix("А123АА7777"))
    }

    @Test fun changingCountryRevalidatesNumber() {
        val ru = PlateNumberValidator(ruRegex)
        val digits = PlateNumberValidator(digitsRegex)
        val number = "А123АА77"
        assertTrue(ru.isComplete(number))
        assertFalse(digits.isComplete(number))
        assertFalse(digits.isValidPrefix(number))
    }

    @Test fun autoEndAddsHoursAcrossMidnight() {
        val start = LocalDateTime.of(2026, 1, 1, 23, 0)
        val end = PlateValidityCalculator.autoEnd(start, 24)
        assertEquals(LocalDateTime.of(2026, 1, 2, 23, 0), end)
    }

    @Test fun autoEndAcrossMonthAndYearRollover() {
        val start = LocalDateTime.of(2026, 12, 31, 20, 0)
        val end = PlateValidityCalculator.autoEnd(start, 6)
        assertEquals(LocalDateTime.of(2027, 1, 1, 2, 0), end)
    }

    @Test fun recomputeOnStartChange() {
        val hours = 5
        val end1 = PlateValidityCalculator.autoEnd(LocalDateTime.of(2026, 5, 10, 10, 0), hours)
        val end2 = PlateValidityCalculator.autoEnd(LocalDateTime.of(2026, 5, 10, 12, 0), hours)
        assertEquals(LocalDateTime.of(2026, 5, 10, 15, 0), end1)
        assertEquals(LocalDateTime.of(2026, 5, 10, 17, 0), end2)
    }

    @Test fun endBeforeStartIsInvalid() {
        val start = LocalDateTime.of(2026, 5, 10, 12, 0)
        val earlier = LocalDateTime.of(2026, 5, 10, 11, 0)
        assertFalse(PlateValidityCalculator.isPeriodValid(start, earlier))
        assertTrue(PlateValidityCalculator.isPeriodValid(start, start))
        assertTrue(PlateValidityCalculator.isPeriodValid(null, earlier))
    }

    @Test fun isoKeepsDeviceTimeZoneOffset() {
        val dateTime = LocalDateTime.of(2026, 10, 14, 18, 30, 0)
        val iso = PlateValidityCalculator.toIso(dateTime, ZoneOffset.ofHours(3))
        assertEquals("2026-10-14T18:30:00+03:00", iso)
    }

    @Test fun flagEmojiComputedFromCodeAndFallsBack() {
        assertEquals("\uD83C\uDDF7\uD83C\uDDFA", PlateCountryCatalog.flagEmoji("ru"))
        assertEquals("\uD83C\uDDE6\uD83C\uDDF2", PlateCountryCatalog.flagEmoji("am"))
        // Неизвестный/некорректный код -> нейтральный белый флаг.
        assertEquals("\uD83C\uDFF3\uFE0F", PlateCountryCatalog.flagEmoji("xyz"))
    }
}
