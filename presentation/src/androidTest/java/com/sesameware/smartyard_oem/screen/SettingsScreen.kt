package com.sesameware.smartyard_oem.screen

import io.github.kakaocup.kakao.image.KImageView
import com.kaspersky.kaspresso.screens.KScreen
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.MainActivity

/**
 * @author Nail Shakurov
 * Created on 06.08.2020.
 */
object SettingsScreen : KScreen<SettingsScreen>() {
    override val layoutId: Int? = R.layout.fragment_settings
    override val viewClass: Class<*>? = MainActivity::class.java
//    val ivSettings = KImageView { withId(R.id.ivSettings) }
}
