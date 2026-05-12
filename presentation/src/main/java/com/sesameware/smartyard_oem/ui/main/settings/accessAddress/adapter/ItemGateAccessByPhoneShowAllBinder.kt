package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sesameware.smartyard_oem.databinding.ItemGateAccessByPhoneShowAllBinding
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.ContactModel

class ItemGateAccessByPhoneShowAllBinder(
    override val itemCount: Int,
    private val onItemClick: () -> Unit
) : FixedListItemBinder<ContactModel> {

    override fun inflate(parent: ViewGroup) {
        ItemGateAccessByPhoneShowAllBinding
            .inflate(LayoutInflater.from(parent.context), parent, true)
    }

    override fun bind(item: ContactModel, position: Int, rootView: View) {
        rootView.setOnClickListener {
            onItemClick.invoke()
        }
    }
}