package ru.madbrains.smartyard.screen

import com.agoda.kakao.common.views.KView
import com.agoda.kakao.dialog.KAlertDialog
import com.agoda.kakao.switch.KSwitch
import com.agoda.kakao.text.KTextView
import com.kaspersky.kaspresso.screens.KScreen
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.MainActivity

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
