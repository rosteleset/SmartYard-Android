package com.sesameware.data.player.log

import timber.log.Timber
import android.util.Log
import com.sesameware.domain.interfaces.PlayerLogRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlayerDebugTree : Timber.Tree(), KoinComponent {

    private val repository: PlayerLogRepository by inject()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (tag != PLAYER_TAG) return

        val level = when (priority) {
            Log.VERBOSE, Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR, Log.ASSERT -> "ERROR"
            else -> "UNKNOWN"
        }

        val finalMessage = if (t != null) "$message \nException: ${t.message}" else message

        repository.addLog(level, finalMessage)
    }

    companion object {
        const val PLAYER_TAG = "PLAYER_DEBUG"
    }
}