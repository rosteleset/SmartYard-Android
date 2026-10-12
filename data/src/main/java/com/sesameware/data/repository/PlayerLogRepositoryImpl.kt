package com.sesameware.data.repository

import com.sesameware.domain.interfaces.PlayerLogRepository
import com.sesameware.domain.model.PlayerLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlayerLogRepositoryImpl : PlayerLogRepository {
    private val maxLogsCount = 500

    private val _logsFlow = MutableStateFlow<List<PlayerLog>>(emptyList())
    override val logsFlow: StateFlow<List<PlayerLog>> = _logsFlow.asStateFlow()

    override fun addLog(level: String, message: String) {
        val newLog = PlayerLog(
            level = level,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        _logsFlow.update { currentLogs ->
            val updatedList = currentLogs + newLog
            if (updatedList.size > maxLogsCount) {
                updatedList.drop(updatedList.size - maxLogsCount)
            } else {
                updatedList
            }
        }
    }

    override fun clear() {
        _logsFlow.value = emptyList()
    }
}