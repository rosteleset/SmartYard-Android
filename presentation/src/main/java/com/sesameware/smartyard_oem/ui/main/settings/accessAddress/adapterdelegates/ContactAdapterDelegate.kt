package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapterdelegates

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.PhoneLookup
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.setTextColorRes
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.ContactModel
import ru.rambler.libs.swipe_layout.SwipeLayout
import java.io.IOException
import java.io.InputStream

/**
 * @author Nail Shakurov
 * Created on 26/02/2020.
 */
class ContactAdapterDelegate(
    var activity: Activity,
    var hideSms: Boolean,
    private val deleteListener: (position: Int, number: String) -> Unit,
    private val smsListener: (number: String) -> Unit
) :
    AdapterDelegate<List<ContactModel>>() {

    private val inflater: LayoutInflater = activity.layoutInflater

    override fun isForViewType(items: List<ContactModel>, position: Int): Boolean =
        true

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AddressCameraViewHolder(
            inflater.inflate(
                R.layout.item_contact_access_address,
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<ContactModel>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        holder as AddressCameraViewHolder
        holder.apply {
            val item: ContactModel = items[position]
            activity.applicationContext?.let {
                var infoContact: Contact? = null
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    infoContact = getInfoNumberContact(
                        item.number,
                        it
                    )
                }

                tvTitle.text = infoContact?.name ?: item.number

                Glide.with(it)
                    .load(infoContact?.avatar)
                    .placeholder(R.drawable.ic_userpic)
                    .apply(RequestOptions.circleCropTransform())
                    .into(holder.ivAvatar)
            }

            tvSms.isVisible = !hideSms

            if (item.isOwner) {
                tvTitle.isEnabled = false
                tvTitle.setTextColorRes(R.color.grey_100)
                swipeLayout.isSwipeEnabled = false

                rightViewDelete.setOnClickListener(null)
                tvSms.setOnClickListener(null)
            } else {
                tvTitle.isEnabled = true
                tvTitle.setTextColorRes(R.color.black)
                swipeLayout.isSwipeEnabled = true

                rightViewDelete.setOnClickListener {
                    holder.swipeLayout.reset()
                    deleteListener.invoke(position, item.number)
                }

                tvSms.setOnClickListener {
                    smsListener.invoke(item.number)
                }
            }
        }
    }

    internal class AddressCameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val rightViewDelete: LinearLayout = itemView.findViewById(R.id.rightViewDelete)
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val swipeLayout: SwipeLayout = itemView.findViewById(R.id.swipeLayout)
        val tvSms: TextView = itemView.findViewById(R.id.tvSms)
    }

    private fun getInfoNumberContact(phoneNumber: String, context: Context): Contact {
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

    private data class Contact(
        var name: String?,
        var avatar: Bitmap?
    )
}
