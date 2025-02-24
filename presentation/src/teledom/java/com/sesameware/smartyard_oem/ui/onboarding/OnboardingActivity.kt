package com.sesameware.smartyard_oem.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ActivityOnboardingBinding
import com.sesameware.smartyard_oem.ui.viewPager2.ZoomOutPageTransformer
import com.sesameware.smartyard_oem.ui.reg.RegistrationActivity

/**
 * @author Artem Budarin
 * Created on 08/05/2020.
 */
class OnboardingActivity : CommonActivity() {
    lateinit var binding: ActivityOnboardingBinding

    override val mViewModel by viewModel<OnboardingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setupPager()

        binding.nextButton.setOnClickListener { mViewModel.onNextClick() }
        binding.completeButton.setOnClickListener { mViewModel.onCompleteClick() }
        binding.skipTextView.setOnClickListener { mViewModel.onSkipClick() }

        mViewModel.navigateToNextPage.observe(
            this,
            EventObserver {
                displayNextPage()
            }
        )
        mViewModel.navigateToRegistration.observe(
            this,
            EventObserver {
                openRegistrationActivity()
            }
        )
    }

    private fun setupPager() {
        val pages = mutableListOf<OnboardingPageModel>().apply {
            add(
                OnboardingPageModel(
                    R.drawable.ic_onboarding_cam,
                    getString(R.string.onboarding_cam_title),
                    getString(R.string.onboarding_cam_subtitle)
                )
            )
            add(
                OnboardingPageModel(
                    R.drawable.ic_onboarding_donation,
                    getString(R.string.onboarding_donation_title),
                    getString(R.string.onboarding_donation_subtitle)
                )
            )
            add(
                OnboardingPageModel(
                    R.drawable.ic_onboarding_intercom,
                    getString(R.string.onboarding_intercom_title),
                    getString(R.string.onboarding_intercom_subtitle)
                )
            )
        }
        binding.pageIndicatorView.count = pages.size
        binding.viewPager.apply {
            setPageTransformer(ZoomOutPageTransformer())
            adapter = OnboardingPageAdapter(pages)
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.pageIndicatorView.selection = position
                if (position == pages.count() - 1) {
                    binding.nextButton.isInvisible = true
                    binding.completeButton.isInvisible = false
                } else {
                    binding.nextButton.isInvisible = false
                    binding.completeButton.isInvisible = true
                }
            }
        })
    }

    private fun displayNextPage() {
        binding.viewPager.currentItem += 1
    }

    private fun openRegistrationActivity() {
        val intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
        finish()
    }
}
