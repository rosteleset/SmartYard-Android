package ru.madbrains.smartyard.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_onboarding.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.CommonActivity
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.viewPager2.ZoomOutPageTransformer
import ru.madbrains.smartyard.ui.reg.RegistrationActivity

/**
 * @author Artem Budarin
 * Created on 08/05/2020.
 */
class OnboardingActivity : CommonActivity() {

    override val mViewModel by viewModel<OnboardingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        setupPager()

        nextButton.setOnClickListener { mViewModel.onNextClick() }
        completeButton.setOnClickListener { mViewModel.onCompleteClick() }
        skipTextView.setOnClickListener { mViewModel.onSkipClick() }

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
        pageIndicatorView.count = pages.size
        viewPager.apply {
            setPageTransformer(ZoomOutPageTransformer())
            adapter = OnboardingPageAdapter(pages)
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pageIndicatorView.selection = position
                if (position == pages.count() - 1) {
                    nextButton.isInvisible = true
                    completeButton.isInvisible = false
                } else {
                    nextButton.isInvisible = false
                    completeButton.isInvisible = true
                }
            }
        })
    }

    private fun displayNextPage() {
        viewPager.currentItem += 1
    }

    private fun openRegistrationActivity() {
        val intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
        finish()
    }
}
