package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogLicensePlateV2

import java.util.regex.Pattern

/**
 * Validates a license plate number against a regular expression received from the server.
 *
 * Two input states are distinguished:
 * - [isComplete] — the text fully matches the format and can be submitted;
 * - [isValidPrefix] — the text is a valid beginning (prefix) of a license plate number, so input can continue.
 *
 * [java.util.regex.Matcher.hitEnd] is used to determine whether the input is a valid prefix.
 * This works reliably with arbitrary regular expressions (alternatives, character classes,
 * quantifiers, positional constraints) and avoids naive character-counting approaches.
 */
class PlateNumberValidator(regexPattern: String) {
    private val pattern: Pattern = Pattern.compile(regexPattern)

    /** A full match against the format; submission is allowed. */
    fun isComplete(input: String): Boolean = pattern.matcher(input).matches()

    /**
     * Checks whether [input] is a valid prefix of a license plate number.
     * An empty string is always valid. Otherwise, the input is valid if it either fully
     * matches the format already or can be extended to match it (the matcher has reached the end of the input).
     */
    fun isValidPrefix(input: String): Boolean {
        if (input.isEmpty()) return true
        val matcher = pattern.matcher(input)
        val matches = matcher.matches()
        return matches || matcher.hitEnd()
    }
}
