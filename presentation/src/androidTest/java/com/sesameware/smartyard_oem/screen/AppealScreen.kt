package com.sesameware.smartyard_oem.screen

import com.agoda.kakao.edit.KEditText
import com.agoda.kakao.text.KButton
import com.kaspersky.kaspresso.screens.KScreen
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.reg.RegistrationActivity

/**
 * @author Nail Shakurov
 * Created on 01.08.2020.
 */

object AppealScreen : KScreen<AppealScreen>() {
    override val layoutId: Int? = R.layout.fragment_appeal
    override val viewClass: Class<*>? = RegistrationActivity::class.java
    val nameText = KEditText { withId(R.id.nameText) }
    val patronymicText = KEditText { withId(R.id.patronymicText) }
    val btnDone = KButton { withId(R.id.btnDone) }
}
