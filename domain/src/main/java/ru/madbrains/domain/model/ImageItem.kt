package ru.madbrains.domain.model

import android.graphics.Bitmap
import java.io.Serializable

data class ImageItem(
    val id: Long,
    val name: String,
    val image: Bitmap,
    val type: Boolean,
) : Serializable

