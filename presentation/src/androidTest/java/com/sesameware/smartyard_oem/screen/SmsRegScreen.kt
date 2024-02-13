package com.sesameware.smartyard_oem.screen

import io.github.kakaocup.kakao.edit.KEditText
import com.kaspersky.kaspresso.screens.KScreen
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.reg.RegistrationActivity

/**
 * @author Nail Shakurov
 * Created on 01.08.2020.
 */
object SmsRegScreen : KScreen<SmsRegScreen>() {
    override val layoutId: Int? = R.layout.fragment_sms_reg
    override val viewClass: Class<*>? = RegistrationActivity::class.java
    val pin = KEditText { withId(R.id.pin) }
}
