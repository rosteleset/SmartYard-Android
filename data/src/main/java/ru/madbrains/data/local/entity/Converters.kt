package ru.madbrains.data.local.entity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.room.TypeConverter
import ru.madbrains.domain.model.StateButton
import java.io.ByteArrayOutputStream


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


class BitmapsConverter {
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?): String? {
        if (bitmap == null) {
            return null
        }
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    @TypeConverter
    fun toBitmap(base64String: String?): Bitmap? {
        if (base64String == null) {
            return null
        }
        val byteArray = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}