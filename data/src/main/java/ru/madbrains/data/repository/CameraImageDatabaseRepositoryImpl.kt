package ru.madbrains.data.repository

import ru.madbrains.data.local.dao.ImageDao
import ru.madbrains.data.local.mapper.toImageEntity
import ru.madbrains.data.local.mapper.toImageItem
import ru.madbrains.data.local.mapper.toItem
import ru.madbrains.domain.interfaces.CameraItemDatabaseRepository
import ru.madbrains.domain.model.ImageItem

class CameraImageDatabaseRepositoryImpl(private val dao: ImageDao) : CameraItemDatabaseRepository {
    override suspend fun getItems(): List<ImageItem> {
        return dao.getAll().map { it.toImageItem() }
    }

    override suspend fun getById(id: Long): ImageItem {
        return dao.getById(id).toImageItem()
    }

    override suspend fun create(imageItem: ImageItem): Long {
        return dao.insert(imageItem.toImageEntity())
    }

    override suspend fun delete(itemId: Long): Boolean {
        return dao.deleteById(itemId) > 0
    }

    override suspend fun deleteAll(): Int {
        return dao.deleteAll()
    }
}