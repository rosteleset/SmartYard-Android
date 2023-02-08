package com.sesameware.smartyard_oem.screen

import com.agoda.kakao.bottomnav.KBottomNavigationView
import com.agoda.kakao.text.KButton
import com.kaspersky.kaspresso.screens.KScreen
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.onboarding.OnboardingActivity

/**
 * @author Nail Shakurov
 * Created on 31.07.2020.
 */
object MainScreen : KScreen<MainScreen>() {
    override val layoutId: Int? = R.layout.activity_onboarding
    override val viewClass: Class<*>? = OnboardingActivity::class.java
    val btnCompleteButton = KButton { withId(R.id.completeButton) }
    val btnSkipTextView = KButton { withId(R.id.skipTextView) }
    val bottomMenu = KBottomNavigationView { withId(R.id.bottom_nav) }
}
