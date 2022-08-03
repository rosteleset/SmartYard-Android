package ru.madbrains.smartyard.ui.main.burger

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BurgerModel(
    val orderId: Int = 0,
    val iconId: Int? = null,
    val iconUrl: String? = null,
    val title: String = "",
    val onClick: () -> Unit
) : Parcelable
