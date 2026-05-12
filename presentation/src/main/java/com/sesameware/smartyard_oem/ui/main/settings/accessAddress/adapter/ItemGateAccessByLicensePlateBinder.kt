package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sesameware.smartyard_oem.databinding.ItemGateAccessByLicensePlateBinding
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.LicensePlateValue

class ItemGateAccessByLicensePlateBinder(
    override val itemCount: Int,
    private val onItemClick: (Int) -> Unit,
    private val onMenuClick: (View, LicensePlateValue) -> Unit
) : FixedListItemBinder<LicensePlateValue> {

    override fun inflate(parent: ViewGroup) {
        ItemGateAccessByLicensePlateBinding
            .inflate(LayoutInflater.from(parent.context), parent, true)
    }

    override fun bind(item: LicensePlateValue, position: Int, rootView: View) {
        with (ItemGateAccessByLicensePlateBinding.bind(rootView)) {
            tvLicensePlate.text = item.value
            ivMenu.setOnClickListener {
                onMenuClick.invoke(it, item)
            }
        }
        rootView.setOnClickListener {
            onItemClick.invoke(position)
        }
    }
}