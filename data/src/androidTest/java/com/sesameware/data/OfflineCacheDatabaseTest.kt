package com.sesameware.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sesameware.data.local.db.ItemsDatabase
import com.sesameware.data.local.entity.OfflineAddressEntity
import com.sesameware.data.local.entity.OfflineDoorEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineCacheDatabaseTest {
    @Test fun migrationPreservesLegacyTableAndRoomValidatesNewSchema() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "offline-migration-test.db"
        context.deleteDatabase(name)
        context.getDatabasePath(name).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE address_door (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, address TEXT NOT NULL, icon TEXT NOT NULL, domophoneId INTEGER NOT NULL, doorId INTEGER, state TEXT NOT NULL)")
            db.execSQL("INSERT INTO address_door (id, name, address, icon, domophoneId, doorId, state) VALUES (1, 'Existing', 'Address', '', 7, 0, 'UNKNOWN')")
            db.version = 4
        }
        val db = Room.databaseBuilder(context, ItemsDatabase::class.java, name)
            .addMigrations(ItemsDatabase.MIGRATION_4_5).build()
        try {
            assertTrue(db.offlineAddressDao().readOfflineData("owner").isEmpty())
            db.openHelper.readableDatabase.query("SELECT name FROM address_door WHERE id = 1").use {
                assertTrue(it.moveToFirst())
                assertEquals("Existing", it.getString(0))
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun replacementKeepsCompositeIdentityAndClearRemovesBothTables() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, ItemsDatabase::class.java).build()
        try {
            val dao = db.offlineAddressDao()
            dao.replaceOfflineData(
                listOf(OfflineAddressEntity(2, "First", 0, "owner"), OfflineAddressEntity(1, "Second", 1, "owner")),
                listOf(OfflineDoorEntity(7, 2, "Entrance", "111", 0, 0),
                    OfflineDoorEntity(7, 2, "Gate", "222", 1, 1),
                    OfflineDoorEntity(7, 1, "Entrance", "333", 0, 0))
            )
            val cached = dao.readOfflineData("owner")
            assertEquals(listOf(2, 1), cached.map { it.houseId })
            assertEquals(listOf("111", "222"), cached.first().doors.map { it.doorCode })
            assertTrue(dao.readOfflineData("other").isEmpty())
            dao.clearOfflineData()
            assertTrue(dao.getAllAddresses().isEmpty())
            assertTrue(dao.getAllDoors().isEmpty())
        } finally { db.close() }
    }
}
