package ru.madbrains.domain.model.response

import com.google.gson.annotations.SerializedName

/**
 * @author Nail Shakurov
 * Created on 25/03/2020.
 */

typealias RoommateResponse = ApiResult<List<ItemRoommate>>

data class ItemRoommate(
    @SerializedName("flatId")
    val flatId: Int = -1, // 1
    @SerializedName("roommates")
    val roommates: List<Settings.Roommate> = listOf()
)
