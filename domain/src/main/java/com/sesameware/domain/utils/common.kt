package com.sesameware.domain.utils

import android.os.Looper

typealias listenerGeneric<T> = (T) -> Unit
typealias listenerGenericR<T, R> = (T) -> R
typealias listenerEmpty = () -> Unit
fun doDelayed(callbackEmpty: listenerEmpty, milli: Long) {
    android.os.Handler(Looper.getMainLooper()).postDelayed({ callbackEmpty() }, milli)
}

infix fun CharSequence?.concatIfNotNullOrBlank(other: CharSequence?): String? {
    return if (!this.isNullOrBlank() && !other.isNullOrBlank()) {
        "$this$other"
    } else {
        null
    }
}

infix fun CharSequence.concatIfNotBlank(other: CharSequence): String {
    return if (!this.isBlank() && !other.isBlank()) {
        "$this$other"
    } else {
        ""
    }
}