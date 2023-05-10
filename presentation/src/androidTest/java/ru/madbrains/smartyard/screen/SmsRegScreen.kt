package ru.madbrains.smartyard.screen

import com.agoda.kakao.edit.KEditText
import com.kaspersky.kaspresso.screens.KScreen
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.reg.RegistrationActivity

/**
 * @author Nail Shakurov
 * Created on 01.08.2020.
 */
object SmsRegScreen : KScreen<SmsRegScreen>() {
    override val layoutId: Int? = R.layout.fragment_sms_reg
    override val viewClass: Class<*>? = RegistrationActivity::class.java
    val pin = KEditText { withId(R.id.pin) }
}
