package com.sesameware.domain.interfaces

import com.sesameware.domain.model.PlayerLog
import kotlinx.coroutines.flow.StateFlow

interface PlayerLogRepository {
    val logsFlow: StateFlow<List<PlayerLog>>
    fun addLog(level: String, message: String)
    fun clear()
}