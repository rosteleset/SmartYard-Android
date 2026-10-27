package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ItemGateAccessByLicensePlateBinding
import com.sesameware.smartyard_oem.ui.countryCodeToFlag
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogLicensePlateV2.PlateValidityCalculator
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
            tvLicensePlate.text = item.number + (if (item.watcherId != null) " \uD83D\uDC41" else "")
            tvCountryFlag.text = countryCodeToFlag(item.countryCode)

            if (item.validFrom != null) {
                tvValidFrom.visibility = View.VISIBLE
                tvValidFrom.text = root.context.getString(R.string.period_from,
                    PlateValidityCalculator.fromIso(item.validFrom))
            } else {
                tvValidFrom.visibility = View.GONE
                tvValidFrom.text = ""
            }

            if (item.validTo != null) {
                tvValidTo.visibility = View.VISIBLE
                tvValidTo.text = root.context.getString(R.string.period_to,
                    PlateValidityCalculator.fromIso(item.validTo))
            } else {
                tvValidTo.visibility = View.GONE
                tvValidTo.text = ""
            }

            ivMenu.setOnClickListener {
                onMenuClick.invoke(it, item)
            }
        }
        rootView.setOnClickListener {
            onItemClick.invoke(position)
        }
    }
}