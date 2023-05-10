package ru.madbrains.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.madbrains.data.local.dao.AddressDao
import ru.madbrains.data.local.entity.AddressDoorEntity
import ru.madbrains.data.local.entity.Converters

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
