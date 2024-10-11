package ru.madbrains.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.madbrains.data.local.entity.AddressDoorEntity
import ru.madbrains.data.local.entity.CameraImageEntity
import ru.madbrains.domain.model.StateButton

@Dao
abstract class ImageDao {
    @Query("SELECT * FROM camera_image WHERE id = :id")
    abstract suspend fun getById(id: Long): CameraImageEntity

    @Query("SELECT * FROM camera_image")
    abstract suspend fun getAll(): List<CameraImageEntity>

    @Query("DELETE FROM camera_image")
    abstract suspend fun deleteAll(): Int

    @Query("DELETE FROM camera_image WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(imageEntity: CameraImageEntity): Long

    @Delete
    abstract suspend fun delete(imageEntity: CameraImageEntity): Int

    @Update
    abstract suspend fun update(imageEntity: CameraImageEntity)

}