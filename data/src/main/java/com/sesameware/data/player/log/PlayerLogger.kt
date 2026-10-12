package com.sesameware.data.player.log

import timber.log.Timber

object PlayerLogger {
    fun d(message: String) = Timber.tag(PlayerDebugTree.PLAYER_TAG).d(message)
    fun i(message: String) = Timber.tag(PlayerDebugTree.PLAYER_TAG).i(message)
    fun w(message: String) = Timber.tag(PlayerDebugTree.PLAYER_TAG).w(message)
    fun e(t: Throwable? = null, message: String) = Timber.tag(PlayerDebugTree.PLAYER_TAG).e(t, message)
}