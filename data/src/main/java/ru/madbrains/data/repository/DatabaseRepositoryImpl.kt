package ru.madbrains.data.repository

import ru.madbrains.data.local.dao.AddressDao
import ru.madbrains.data.local.mapper.toItem
import ru.madbrains.data.local.mapper.toItemEntity
import ru.madbrains.domain.interfaces.DatabaseRepository
import ru.madbrains.domain.model.AddressItem
import ru.madbrains.domain.model.StateButton

class DatabaseRepositoryImpl(private val dao: AddressDao) : DatabaseRepository {
    override fun getItems(): List<AddressItem> {
        return dao.getAll().map { it.toItem() }
    }

    override suspend fun create(addressItem: AddressItem): Long {
        return dao.insert(addressItem.toItemEntity())
    }

    override suspend fun delete(itemId: Long): Boolean {
        return dao.deleteById(itemId) > 0
    }

    override suspend fun deleteAll(): Int {
        return dao.deleteAll()
    }

    override suspend fun updateState(state: StateButton, id: Int) {
        return dao.updateItemState(state, id)
    }
}
