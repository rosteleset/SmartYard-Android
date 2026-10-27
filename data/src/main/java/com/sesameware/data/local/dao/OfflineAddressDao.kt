package com.sesameware.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sesameware.data.local.entity.OfflineAddressEntity
import com.sesameware.data.local.entity.OfflineDoorEntity
import com.sesameware.domain.model.OfflineAddress
import com.sesameware.domain.model.OfflineDoor

@Dao
abstract class OfflineAddressDao {

    @Query("SELECT * FROM offline_address ORDER BY position")
    abstract suspend fun getAllAddresses(): List<OfflineAddressEntity>

    @Query("SELECT * FROM offline_door WHERE house_id = :houseId")
    abstract suspend fun getDoorsForAddress(houseId: Int): List<OfflineDoorEntity>

    @Query("SELECT * FROM offline_door ORDER BY position")
    abstract suspend fun getAllDoors(): List<OfflineDoorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAddresses(addresses: List<OfflineAddressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDoors(doors: List<OfflineDoorEntity>)

    @Query("DELETE FROM offline_address")
    abstract suspend fun deleteAllAddresses()

    @Query("DELETE FROM offline_door")
    abstract suspend fun deleteAllDoors()

    @Transaction
    open suspend fun readOfflineData(owner: String): List<OfflineAddress> {
        val doors = getAllDoors().groupBy { it.houseId }
        return getAllAddresses().filter { it.owner == owner }.map { address ->
            OfflineAddress(
                address.houseId, address.address,
                doors[address.houseId].orEmpty().filter { it.doorCode.isNotBlank() }.map {
                    OfflineDoor(it.domophoneId, it.name, it.doorCode)
                }
            )
        }
    }

    @Transaction
    open suspend fun clearOfflineData() {
        deleteAllDoors()
        deleteAllAddresses()
    }

    @Transaction
    open suspend fun replaceOfflineData(
        addresses: List<OfflineAddressEntity>,
        doors: List<OfflineDoorEntity>
    ) {
        clearOfflineData()
        if (addresses.isNotEmpty()) {
            insertAddresses(addresses)
        }
        if (doors.isNotEmpty()) {
            insertDoors(doors)
        }
    }
}
