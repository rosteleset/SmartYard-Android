package ru.madbrains.domain.utils

typealias listenerGeneric<T> = (T) -> Unit
typealias listenerGenericA<T, U> = (T, U) -> Unit
typealias listenerGenericR<T, R> = (T) -> R
typealias listenerEmpty = () -> Unit
fun doDelayed(callbackEmpty: listenerEmpty, milli: Long) {
    android.os.Handler().postDelayed({ callbackEmpty() }, milli)
}
