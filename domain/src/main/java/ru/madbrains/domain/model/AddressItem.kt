package ru.madbrains.domain.model

import java.io.Serializable

data class AddressItem(
    val id: Long = 0,
    val name: String,
    val address: String,
    val icon: String,
    val domophoneId: Long,
    val doorId: Int?,
    var state: StateButton
) : Serializable

enum class StateButton {
    OPEN, CLOSE
}
