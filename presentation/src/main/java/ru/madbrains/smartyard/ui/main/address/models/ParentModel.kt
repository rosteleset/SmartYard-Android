package ru.madbrains.smartyard.ui.main.address.models

import ru.madbrains.smartyard.ui.main.address.models.interfaces.DisplayableItem

data class ParentModel(
    val addressTitle: String = "",
    val houseId: Int = 0,
    val children: List<DisplayableItem>,
    val hasYards: Boolean = false  // для сортировки списка адресов (если есть что открывать, то такие адреса идут в начале списка)
) : DisplayableItem
