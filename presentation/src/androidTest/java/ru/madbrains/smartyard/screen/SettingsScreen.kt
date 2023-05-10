package ru.madbrains.smartyard.screen

import com.agoda.kakao.image.KImageView
import com.kaspersky.kaspresso.screens.KScreen
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.MainActivity

/**
 * @author Nail Shakurov
 * Created on 06.08.2020.
 */
object SettingsScreen : KScreen<SettingsScreen>() {
    override val layoutId: Int? = R.layout.fragment_settings
    override val viewClass: Class<*>? = MainActivity::class.java
    val ivSettings = KImageView { withId(R.id.ivSettings) }
}
