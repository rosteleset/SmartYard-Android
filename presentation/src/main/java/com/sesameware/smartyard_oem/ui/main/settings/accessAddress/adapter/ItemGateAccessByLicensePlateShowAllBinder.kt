package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sesameware.smartyard_oem.databinding.ItemGateAccessByLicensePlateShowAllBinding
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.LicensePlateValue

class ItemGateAccessByLicensePlateShowAllBinder(
    override val itemCount: Int,
    private val onItemClick: () -> Unit
) : FixedListItemBinder<LicensePlateValue> {

    override fun inflate(parent: ViewGroup) {
        ItemGateAccessByLicensePlateShowAllBinding
            .inflate(LayoutInflater.from(parent.context), parent, true)
    }

    override fun bind(item: LicensePlateValue, position: Int, rootView: View) {
        rootView.setOnClickListener {
            onItemClick.invoke()
        }
    }
}