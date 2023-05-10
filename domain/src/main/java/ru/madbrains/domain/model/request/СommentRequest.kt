package ru.madbrains.domain.model.request

import com.squareup.moshi.Json
/**
 * @author Nail Shakurov
 * Created on 28.04.2020.
 */
data class СommentRequest(
    @Json(name = "comment")
    val comment: String = "", // Cменился способ доставки. Подготовить пакет для курьера.
    @Json(name = "key")
    val key: String = "" // REM-418379
)
