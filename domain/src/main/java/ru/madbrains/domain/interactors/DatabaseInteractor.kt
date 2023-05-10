package ru.madbrains.domain.interactors

import ru.madbrains.domain.interfaces.DatabaseRepository
import ru.madbrains.domain.model.AddressItem
import ru.madbrains.domain.model.StateButton

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
