package com.sesameware.smartyard_oem.screen

import com.kaspersky.kaspresso.screens.KScreen
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.MainActivity
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.dialog.KAlertDialog
import io.github.kakaocup.kakao.switch.KSwitch
import io.github.kakaocup.kakao.text.KTextView

/**
 * @author Nail Shakurov
 * Created on 06.08.2020.
 */
object BasicSettingsScreen : KScreen<BasicSettingsScreen>() {
    override val layoutId: Int = R.layout.fragment_basic_settings
    override val viewClass: Class<*> = MainActivity::class.java
    val cvExit = KView { withId(R.id.cvExit) }
    val tvShowNotify = KTextView { withId(R.id.tvShowNotify) }
    val swShowNotify = KSwitch { withId(R.id.swShowNotify) }
    val sBalanse = KSwitch { withId(R.id.sBalance) }
    val alertDialog = KAlertDialog()
}
