package ru.madbrains.data.local.mapper

import ru.madbrains.data.local.entity.AddressDoorEntity
import ru.madbrains.data.local.entity.CameraImageEntity
import ru.madbrains.domain.model.AddressItem
import ru.madbrains.domain.model.ImageItem

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





private fun map(item: CameraImageEntity) = ImageItem(
    id = item.id,
    name = item.name,
    image = item.image,
    type = item.type
)

fun CameraImageEntity.toImageItem() = map(this)

fun List<CameraImageEntity>.toImageItem() = map { it.toImageItem() }

private fun map(imageItem: ImageItem) = CameraImageEntity(
    id = imageItem.id,
    name = imageItem.name,
    image = imageItem.image,
    type = imageItem.type
)

fun ImageItem.toImageEntity() = map(this)
fun List<ImageItem>.toImageEntity() = map { it.toImageEntity() }
