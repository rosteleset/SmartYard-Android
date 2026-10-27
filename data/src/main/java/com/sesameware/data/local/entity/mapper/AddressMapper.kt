package com.sesameware.data.local.entity.mapper

import com.sesameware.data.local.entity.AddressDoorEntity
import com.sesameware.data.local.entity.OfflineAddressEntity
import com.sesameware.data.local.entity.OfflineDoorEntity
import com.sesameware.domain.model.AddressItem
import com.sesameware.domain.model.response.Address

/**
 * @author Nail Shakurov
 * Created on 14.05.2020.
 */
// TODO Refactor on whole DTO persistence
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

fun Address.toOfflineDoorEntities(): List<OfflineDoorEntity> =
    doors.mapIndexed { index, door ->
        OfflineDoorEntity(
            domophoneId = door.domophoneId,
            houseId = this.houseId,
            name = door.name,
            doorCode = door.doorCode.orEmpty(),
            doorId = door.doorId,
            position = index
        )
    }

fun Address.toOfflineAddressEntity(index: Int, owner: String): OfflineAddressEntity =
    OfflineAddressEntity(houseId, address, index, owner)
