package ru.madbrains.domain.interfaces

import ru.madbrains.domain.model.AddressItem
import ru.madbrains.domain.model.StateButton

/**
 * @author Nail Shakurov
 * Created on 14.05.2020.
 */
interface DatabaseRepository {

    fun getItems(): List<AddressItem>

    suspend fun create(addressItem: AddressItem): Long

    suspend fun delete(itemId: Long): Boolean

    suspend fun deleteAll(): Int

    suspend fun updateState(state: StateButton, id: Int)
}
