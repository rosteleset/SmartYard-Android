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
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemContactAccessAddressBinding
import com.sesameware.smartyard_oem.setTextColorRes
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.ItemContactDelegate
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.ContactModel
import org.koin.java.KoinJavaComponent.injectOrNull
import java.io.IOException
import java.io.InputStream

/**
 * @author Nail Shakurov
 * Created on 26/02/2020.
 */
class ContactAdapter(
    var activity: Activity,
    var hideSms: Boolean,
    private val deleteListener: (position: Int, number: String) -> Unit,
    private val smsListener: (number: String) -> Unit
) : ListAdapter<ContactModel, ContactAdapter.AddressCameraViewHolder>(DiffCallback) {

    private val delegate: ItemContactDelegate?
        by injectOrNull(ItemContactDelegate::class.java)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddressCameraViewHolder {
        val binding = ItemContactAccessAddressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddressCameraViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AddressCameraViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class AddressCameraViewHolder(private val binding: ItemContactAccessAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContactModel) {
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

                binding.tvTitle.text = infoContact?.name ?: item.number

                Glide.with(it)
                    .load(infoContact?.avatar)
                    .placeholder(R.drawable.ic_userpic)
                    .apply(RequestOptions.circleCropTransform())
                    .into(binding.ivAvatar)
            }

            binding.tvSms.isVisible = !hideSms

            if (item.isOwner) {
                binding.tvTitle.isEnabled = false
                binding.tvTitle.setTextColorRes(R.color.no_accent)
                binding.swipeLayout.isSwipeEnabled = false
                binding.rightViewDelete.setOnClickListener(null)
                binding.tvSms.setOnClickListener(null)
            } else {
                binding.tvTitle.isEnabled = true
                binding.tvTitle.setTextColorRes(R.color.accent)
                binding.swipeLayout.isSwipeEnabled = true
                binding.rightViewDelete.setOnClickListener {
                    binding.swipeLayout.reset()
                    deleteListener.invoke(position, item.number)
                }
                binding.tvSms.setOnClickListener {
                    smsListener.invoke(item.number)
                }
            }

            delegate?.extendConfig(binding)
        }
    }

    private data class Contact(
        val name: String?,
        val avatar: Bitmap?
    )

    companion object DiffCallback : DiffUtil.ItemCallback<ContactModel>() {
        override fun areItemsTheSame(
            oldItem: ContactModel,
            newItem: ContactModel
        ): Boolean = oldItem.number == newItem.number

        override fun areContentsTheSame(
            oldItem: ContactModel,
            newItem: ContactModel
        ): Boolean = oldItem == newItem

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
    }
}
