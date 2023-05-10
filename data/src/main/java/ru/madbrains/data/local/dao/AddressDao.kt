package ru.madbrains.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.madbrains.data.local.entity.AddressDoorEntity
import ru.madbrains.domain.model.StateButton

@Dao
abstract class AddressDao {

    @Query("SELECT * FROM address_door WHERE id = :id")
    abstract suspend fun getById(id: Long): AddressDoorEntity

    @Query("SELECT * FROM address_door")
    abstract fun getAll(): List<AddressDoorEntity>

    @Query("DELETE FROM address_door")
    abstract suspend fun deleteAll(): Int

    @Query("DELETE FROM address_door WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(addressDoor: AddressDoorEntity): Long

    @Delete
    abstract suspend fun delete(addressDoor: AddressDoorEntity): Int

    @Update
    abstract suspend fun update(addressDoor: AddressDoorEntity)

    @Query("UPDATE address_door SET state = :state WHERE id == :id")
    abstract fun updateItemState(state: StateButton, id: Int)
}
