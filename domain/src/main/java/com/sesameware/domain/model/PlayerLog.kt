package com.sesameware.domain.model

data class PlayerLog(
    val timestamp: Long,
    val level: String,
    val message: String
)