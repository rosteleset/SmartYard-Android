package com.sesameware.smartyard_oem.ui.onboarding

import com.sesameware.smartyard_oem.databinding.ActivityOnboardingBinding

interface OnboardingDelegate {
    fun extendConfig(
        binding: ActivityOnboardingBinding,
        pages: MutableList<OnboardingPageModel>
    )
}