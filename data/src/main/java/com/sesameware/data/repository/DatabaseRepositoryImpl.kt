package com.sesameware.data.repository

import com.sesameware.data.local.dao.AddressDao
import com.sesameware.data.local.mapper.toItem
import com.sesameware.data.local.mapper.toItemEntity
import com.sesameware.domain.interfaces.DatabaseRepository
import com.sesameware.domain.model.AddressItem
import com.sesameware.domain.model.StateButton

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
