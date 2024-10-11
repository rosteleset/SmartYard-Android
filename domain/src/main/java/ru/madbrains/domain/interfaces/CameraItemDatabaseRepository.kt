package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.ImageItem

interface CameraItemDatabaseRepository {

    suspend fun getItems(): List<ImageItem>

    suspend fun getById(id: Long): ImageItem

    suspend fun create(imageItem: ImageItem): Long

    suspend fun delete(itemId: Long): Boolean

    suspend fun deleteAll(): Int

}