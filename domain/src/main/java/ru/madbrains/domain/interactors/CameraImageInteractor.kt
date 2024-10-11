package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.CameraItemDatabaseRepository
import ru.madbrains.domain.model.AddressItem
import ru.madbrains.domain.model.ImageItem
import ru.madbrains.domain.model.StateButton

class CameraImageInteractor(
    private val repository: CameraItemDatabaseRepository
) {
    suspend fun getImageItemList(): List<ImageItem> {
        return repository.getItems()
    }

    suspend fun getImageItemById(id: Long): ImageItem{
        return repository.getById(id)
    }

    suspend fun createItem(item: ImageItem) {
        repository.create(item)
    }

    suspend fun deleteAll(): Int {
        return repository.deleteAll()
    }
}