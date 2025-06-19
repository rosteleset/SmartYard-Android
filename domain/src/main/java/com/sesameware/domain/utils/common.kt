package com.sesameware.domain.utils

import android.os.Looper

typealias listenerGeneric<T> = (T) -> Unit
typealias listenerGenericR<T, R> = (T) -> R
typealias listenerEmpty = () -> Unit
fun doDelayed(callbackEmpty: listenerEmpty, milli: Long) {
    android.os.Handler(Looper.getMainLooper()).postDelayed({ callbackEmpty() }, milli)
}
