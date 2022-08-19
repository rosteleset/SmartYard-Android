package com.sesameware.data.local.entity

import androidx.room.TypeConverter
import com.sesameware.domain.model.StateButton

/**
 * @author Nail Shakurov
 * Created on 16.05.2020.
 */
class Converters {

    @TypeConverter
    fun stateButtonEnumToTnt(value: StateButton) = value.name

    @TypeConverter
    fun intToStateButtonEnum(value: String) = enumValueOf<StateButton>(value)
}
