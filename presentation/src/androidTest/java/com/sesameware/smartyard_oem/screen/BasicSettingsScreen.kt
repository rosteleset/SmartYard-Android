package com.sesameware.smartyard_oem.screen

import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.dialog.KAlertDialog
import io.github.kakaocup.kakao.switch.KSwitch
import io.github.kakaocup.kakao.text.KTextView
import com.kaspersky.kaspresso.screens.KScreen
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.MainActivity

/**
 * @author Nail Shakurov
 * Created on 06.08.2020.
 */
object BasicSettingsScreen : KScreen<BasicSettingsScreen>() {
    override val layoutId: Int? = R.layout.fragment_basic_settings
    override val viewClass: Class<*>? = MainActivity::class.java
    val cvExit = KView { withId(R.id.cvExit) }
    val tvTitleNotif = KTextView { withId(R.id.tvTitleNotif) }
    val tvShowNotify = KTextView { withId(R.id.tvShowNotify) }
    val swShowNotify = KSwitch { withId(R.id.swShowNotify) }
    val sBalanse = KSwitch { withId(R.id.sBalanse) }
    val alertDialog = KAlertDialog()
}
