package ru.madbrains.data.local.mapper

import ru.madbrains.data.local.entity.AddressDoorEntity
import ru.madbrains.domain.model.AddressItem

/**
 * @author Nail Shakurov
 * Created on 14.05.2020.
 */
private fun map(item: AddressDoorEntity) = AddressItem(
    id = item.id,
    name = item.name,
    address = item.address,
    icon = item.icon,
    domophoneId = item.domophoneId,
    doorId = item.doorId,
    state = item.state
)

fun AddressDoorEntity.toItem() = map(this)
fun List<AddressDoorEntity>.toItem() = map { it.toItem() }

private fun map(addressItem: AddressItem) = AddressDoorEntity(
    name = addressItem.name,
    address = addressItem.address,
    icon = addressItem.icon,
    domophoneId = addressItem.domophoneId,
    doorId = addressItem.doorId,
    state = addressItem.state
)

fun AddressItem.toItemEntity() = map(this)
fun List<AddressItem>.toItemEntity() = map { it.toItemEntity() }
