package com.sesameware.smartyard_oem.screen

import com.kaspersky.kaspresso.screens.KScreen
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.reg.RegistrationActivity

/**
 * @author Nail Shakurov
 * Created on 01.08.2020.
 */
object NumberRegScreen : KScreen<NumberRegScreen>() {
    override val layoutId: Int? = R.layout.fragment_number_reg
    override val viewClass: Class<*>? = RegistrationActivity::class.java
//    val tel1 = KEditText { withId(R.id.tel1) }
//    val tel2 = KEditText { withId(R.id.tel2) }
//    val tel3 = KEditText { withId(R.id.tel2) }
}
