package com.sesameware.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "offline_door", primaryKeys = ["house_id", "domophone_id", "door_id"])
data class OfflineDoorEntity(
    @ColumnInfo(name = "domophone_id")
    val domophoneId: Int,

    @ColumnInfo(name = "house_id")
    val houseId: Int,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "door_code")
    val doorCode: String,

    @ColumnInfo(name = "door_id")
    val doorId: Int,

    @ColumnInfo(name = "position")
    val position: Int
)
