package com.sesameware.domain.model

import timber.log.Timber
import java.net.URI

class example
enum class TF(var value: String) {
    TRUE("t"),
    FALSE("f");

    companion object {
        fun getBoolean(flag: Boolean): TF {
            return if (flag) TRUE else FALSE
        }

        fun getString(value: String): Boolean {
            return value == TRUE.value
        }
    }
}

fun concatIfCorrectUrl(url: String, suffix: String): String {
    return if (url.isCorrectUrl()) {
        "${url.trimEnd('/')}$suffix"
    } else {
        Timber.d("debug_dmm incorrect URL: $url")
        ""
    }
}

fun String.isCorrectUrl() = runCatching { URI(this).toURL() }.isSuccess
