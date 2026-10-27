package com.sesameware.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sesameware.data.local.dao.AddressDao
import com.sesameware.data.local.dao.OfflineAddressDao
import com.sesameware.data.local.entity.AddressDoorEntity
import com.sesameware.data.local.entity.Converters
import com.sesameware.data.local.entity.OfflineAddressEntity
import com.sesameware.data.local.entity.OfflineDoorEntity

/**
 * @author Nail Shakurov
 * Created on 14.05.2020.
 */
@Database(
    version = 5,
    entities = [AddressDoorEntity::class, OfflineAddressEntity::class, OfflineDoorEntity::class]
)
@TypeConverters(Converters::class)
abstract class ItemsDatabase : RoomDatabase() {

    abstract fun itemDao(): AddressDao
    abstract fun offlineAddressDao(): OfflineAddressDao

    companion object {
        const val DATABASE_NAME = "database.db"

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS offline_address (house_id INTEGER NOT NULL, address TEXT NOT NULL, position INTEGER NOT NULL, owner TEXT NOT NULL, PRIMARY KEY(house_id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS offline_door (domophone_id INTEGER NOT NULL, house_id INTEGER NOT NULL, name TEXT NOT NULL, door_code TEXT NOT NULL, door_id INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(house_id, domophone_id, door_id))")
            }
        }
    }
}
