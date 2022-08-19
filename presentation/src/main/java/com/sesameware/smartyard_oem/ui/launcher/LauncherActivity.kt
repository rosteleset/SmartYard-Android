package com.sesameware.smartyard_oem.ui.launcher

import android.content.Intent
import android.os.Bundle
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.ui.onboarding.OnboardingActivity
import com.sesameware.smartyard_oem.ui.reg.RegistrationActivity

class LauncherActivity : CommonActivity() {

    override val mViewModel by viewModel<LauncherViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewModel.launchDestination.observe(
            this,
            EventObserver { destination ->
                when (destination) {
                    LauncherViewModel.LaunchDestination.ONBOARDING_ACTIVITY -> openOnboardingActivity()
                    LauncherViewModel.LaunchDestination.REGISTRATION_ACTIVITY -> openRegistrationActivity()
                }
            }
        )
    }

    private fun openOnboardingActivity() {
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openRegistrationActivity() {
        val intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
        finish()
    }
}
