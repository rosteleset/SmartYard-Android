package com.sesameware.domain.model

data class OfflineAddress(
    val houseId: Int,
    val address: String,
    val doors: List<OfflineDoor>
) {
    var isExpanded: Boolean = false
}

data class OfflineDoor(
    val domophoneId: Int,
    val name: String,
    val doorCode: String
)
