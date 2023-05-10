package ru.madbrains.smartyard.ui.launcher

import android.content.Intent
import android.os.Bundle
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.CommonActivity
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.ui.onboarding.OnboardingActivity
import ru.madbrains.smartyard.ui.reg.RegistrationActivity

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
