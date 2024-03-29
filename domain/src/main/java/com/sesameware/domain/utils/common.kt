package com.sesameware.domain.utils

typealias listenerGeneric<T> = (T) -> Unit
typealias listenerGenericR<T, R> = (T) -> R
typealias listenerEmpty = () -> Unit
fun doDelayed(callbackEmpty: listenerEmpty, milli: Long) {
    android.os.Handler().postDelayed({ callbackEmpty() }, milli)
}
