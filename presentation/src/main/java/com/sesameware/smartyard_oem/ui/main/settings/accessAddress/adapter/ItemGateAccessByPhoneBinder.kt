package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sesameware.data.DataModule
import com.sesameware.smartyard_oem.databinding.ItemGateAccessByPhoneBinding
import com.sesameware.smartyard_oem.ui.formatPhoneWith
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.getContact
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.ContactModel

class ItemGateAccessByPhoneBinder(
    override val itemCount: Int,
    private val onItemClick: (Int) -> Unit,
    private val onMenuClick: (View, ContactModel) -> Unit
) : FixedListItemBinder<ContactModel> {

    override fun inflate(parent: ViewGroup) {
        ItemGateAccessByPhoneBinding
            .inflate(LayoutInflater.from(parent.context), parent, true)
    }

    override fun bind(item: ContactModel, position: Int, rootView: View) {
        with (ItemGateAccessByPhoneBinding.bind(rootView)) {
            val contact = getContact(root.context, item.number)
            tvContact.text = contact?.name ?: item.number.formatPhoneWith(DataModule.phonePattern)
            ivMenu.setOnClickListener {
                onMenuClick.invoke(it, item)
            }
            contact?.let {
                Glide.with(root.context)
                    .load(it.avatar)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivAuthorizedUserPic)
            }
        }
        rootView.setOnClickListener {
            onItemClick.invoke(position)
        }
    }
}