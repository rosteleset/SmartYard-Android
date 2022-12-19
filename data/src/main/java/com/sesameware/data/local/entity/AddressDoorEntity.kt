package com.sesameware.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sesameware.domain.model.StateButton

/**
 * @author Nail Shakurov
 * Created on 14.05.2020.
 */
@Entity(tableName = "address_door")
data class AddressDoorEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "domophoneId")
    val domophoneId: Int,

    @ColumnInfo(name = "doorId")
    val doorId: Int?,

    @ColumnInfo(name = "state")
    val state: StateButton
)
