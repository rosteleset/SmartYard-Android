package com.sesameware.domain.interactors

import com.sesameware.domain.interfaces.DatabaseRepository
import com.sesameware.domain.model.AddressItem
import com.sesameware.domain.model.StateButton

/**
 * @author Nail Shakurov
 * Created on 14.05.2020.
 */
class DatabaseInteractor(
    private val repository: DatabaseRepository
) {
    fun getAddressList(): List<AddressItem> {
        return repository.getItems()
    }

    suspend fun createItem(item: AddressItem) {
        repository.create(item)
    }

    suspend fun deleteAll(): Int {
        return repository.deleteAll()
    }

    suspend fun updateState(state: StateButton, id: Int) {
        return repository.updateState(state, id)
    }
}
