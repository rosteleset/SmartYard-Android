package com.sesameware.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sesameware.data.local.dao.AddressDao
import com.sesameware.data.local.entity.AddressDoorEntity
import com.sesameware.data.local.entity.Converters

/**
 * @author Nail Shakurov
 * Created on 14.05.2020.
 */
@Database(
    version = 4,
    entities = [AddressDoorEntity::class]
)
@TypeConverters(Converters::class)
abstract class ItemsDatabase : RoomDatabase() {

    abstract fun itemDao(): AddressDao

    companion object {
        const val DATABASE_NAME = "database.db"
    }
}
