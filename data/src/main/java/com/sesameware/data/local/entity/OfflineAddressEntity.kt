package com.sesameware.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_address")
data class OfflineAddressEntity(
    @PrimaryKey
    @ColumnInfo(name = "house_id")
    val houseId: Int,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "owner")
    val owner: String
)
