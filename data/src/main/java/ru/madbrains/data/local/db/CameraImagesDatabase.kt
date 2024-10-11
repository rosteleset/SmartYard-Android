package ru.madbrains.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.madbrains.data.local.dao.ImageDao
import ru.madbrains.data.local.entity.BitmapsConverter
import ru.madbrains.data.local.entity.CameraImageEntity


@Database(
    version = 8,
    entities = [CameraImageEntity::class]
)
@TypeConverters(BitmapsConverter::class)
abstract class CameraImagesDatabase : RoomDatabase() {

    abstract fun imageDao(): ImageDao

    companion object {
        const val DATABASE_NAME = "database.db"
    }
}