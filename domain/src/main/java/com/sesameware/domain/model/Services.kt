package com.sesameware.domain.model

import androidx.annotation.StringRes
import com.sesameware.domain.R

/**
 * @author Nail Shakurov
 * Created on 11/03/2020.
 */
enum class Services(val value: String, @StringRes val nameId: Int) {
    Internet("internet", R.string.type_internet),
    Iptv("iptv", R.string.type_iptv),
    Ctv("ctv", R.string.type_ctv),
    Phone("phone", R.string.type_phone),
    Cctv("cctv", R.string.type_cctv),
    Domophone("domophone", R.string.type_domophone),
    Gsm("gsm", R.string.type_gsm)
}
