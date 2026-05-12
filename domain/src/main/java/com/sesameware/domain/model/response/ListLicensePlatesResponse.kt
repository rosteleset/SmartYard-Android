package com.sesameware.domain.model.response

import java.util.Locale

typealias ListLicensePlatesResponse = ApiResult<List<LicensePlate>>?

class LicensePlate {
    val value: String

    constructor(input: String?) {
        val normalized = normalizeInput(input)
        throwIfInvalid(normalized)
        value = normalized
    }

    private fun normalizeInput(input: String?): String {
        return input.orEmpty().uppercase(Locale("ru", "RU"))
            .replace("[^А-Я0-9]".toRegex(), "")
            .replace('Ё', 'Е')
    }

    private fun throwIfInvalid(normalized: String) {
        if (normalized.length !in 8..9) throw IllegalArgumentException(
                """
                    The license plate should consist of 8 or 9 characters.
                    License plate passed: $normalized.
                """.trimIndent()
        )

        val matchesPattern = allowedPatterns.any { it.matches(normalized) }
        if (!matchesPattern) throw IllegalArgumentException(
                """
                    The vehicle's license plate must consist of 1 permitted sign, three digits, 
                    two more permitted signs, and a three- or two-digit area code.
                    For example A123AA777.
                    License plate passed: $normalized.
                """.trimIndent()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LicensePlate

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "LicensePlate(value='$value')"
    }

    companion object {
        const val ALLOWED_LETTERS = "АВЕКМНОРСТУХ"
        val allowedPatterns = listOf(
            // Rus car
            Regex("[$ALLOWED_LETTERS][0-9]{3}[$ALLOWED_LETTERS]{2}[0-9]{2,3}"),
            // Rus bike
            // Regex("[0-9]{4}[$ALLOWED_LETTERS]{2}[0-9]{2,3}")
        )

    }
}

object LicensePlateMapper {
    private val cyrillicToLatin = mapOf(
        'А' to 'A', 'В' to 'B', 'Е' to 'E', 'К' to 'K',
        'М' to 'M', 'Н' to 'H', 'О' to 'O', 'Р' to 'P',
        'С' to 'C', 'Т' to 'T', 'У' to 'Y', 'Х' to 'X'
    )

    private val latinToCyrillic = cyrillicToLatin.entries.associate { (k, v) -> v to k }

    private fun convert(value: String, dictionary: Map<Char, Char>): String {
        return buildString(value.length) {
            for (char in value) {
                val upperChar = char.uppercaseChar()
                val mappedChar = dictionary[upperChar] ?: upperChar
                append(mappedChar)
            }
        }
    }

    fun String.cyrillicLettersToLatin(): String = convert(this, cyrillicToLatin)

    fun String.latinLettersToCyrillic(): String = convert(this, latinToCyrillic)
}