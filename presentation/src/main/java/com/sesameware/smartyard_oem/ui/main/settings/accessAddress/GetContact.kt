package com.sesameware.smartyard_oem.ui.main.settings.accessAddress

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.PhoneLookup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.sesameware.smartyard_oem.R
import java.io.IOException
import java.io.InputStream

fun getContact(context: Context, phoneNumber: String): Contact? {
    if (
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS)
        != PackageManager.PERMISSION_GRANTED
    ) return null

    var photo = AppCompatResources.getDrawable(
        context, R.drawable.ic_userpic
    )?.toBitmap()

    var contactName: String? = null
    var contactId: String? = null

    val uri = Uri.withAppendedPath(
        PhoneLookup.CONTENT_FILTER_URI,
        Uri.encode(phoneNumber)
    )
    val projection =
        arrayOf(PhoneLookup.DISPLAY_NAME, PhoneLookup._ID)

    val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(0)
            contactId =
                cursor.getString(cursor.getColumnIndexOrThrow(PhoneLookup._ID))
        }
        cursor.close()

        if (contactId != null) {
            try {
                val inputStream: InputStream? = Contacts.openContactPhotoInputStream(
                    context.contentResolver,
                    ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId.toLong())
                )
                if (inputStream != null) {
                    photo = BitmapFactory.decodeStream(inputStream)
                }
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    return Contact(contactName, photo)
}

data class Contact(
    var name: String?,
    var avatar: Bitmap?
)