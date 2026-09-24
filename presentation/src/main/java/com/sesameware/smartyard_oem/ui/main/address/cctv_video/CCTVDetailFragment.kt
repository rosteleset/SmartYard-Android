package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCctvDetailBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.main.address.addressVerification.TabAdapter
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail.CCTVOnlineTabFragment
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail.arhive.CCTVArchiveTabCalendarFragment
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.koin.java.KoinJavaComponent.injectOrNull
import timber.log.Timber

class CCTVDetailFragment : Fragment() {
    private var _binding: FragmentCctvDetailBinding? = null
    private val binding get() = _binding!!
    private val delegate: CctvDetailDelegate? by injectOrNull(CctvDetailDelegate::class.java)

    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCctvDetailBinding.inflate(inflater, container, false)
        Timber.d("debug_dmm onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("debug_dmm onViewCreated")
        setupUi(childFragmentManager)
        setupObserve()
    }

    private fun setupUi(fm: FragmentManager) {
        binding.contentWrap.applyBottomNavInsetsToPadding()
        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        val adapter = TabAdapter(fm)
        Timber.d("debug_dmm __new adapter CCTVDetailFragment")
        adapter.addFragment(
            CCTVOnlineTabFragment.newInstance(),
            resources.getString(R.string.cctv_detail_tab_online)
        )
        adapter.addFragment(
            CCTVArchiveTabCalendarFragment.newInstance(),
            resources.getString(R.string.cctv_detail_tab_archive)
        )
        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.viewPager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mCCTVViewModel.setCurrentTabPosition(position)
                setupTabs(adapter, position)
            }
        })
        binding.viewPager.setCurrentItem(mCCTVViewModel.currentTabId, false)

        delegate?.extendConfig(binding)
    }

    private fun setupTabs(adapter: TabAdapter, position: Int) {
        val onlineTabFragment =
            adapter.getItem(CCTVViewModel.ONLINE_TAB_POSITION) as CCTVOnlineTabFragment
        when (position) {
            CCTVViewModel.ARCHIVE_TAB_POSITION -> {
                Timber.d("__Q__   call onlineTabFragment.releasePlayer from setupTabs")
                onlineTabFragment.releasePlayer()
            }
            CCTVViewModel.ONLINE_TAB_POSITION -> {
                mCCTVViewModel.chosenCamera.postValue(mCCTVViewModel.chosenCamera.value)
            }
        }
    }

    private fun setupObserve() {
        mCCTVViewModel.chosenCamera.observe(
            viewLifecycleOwner
        ) {
            binding.tvTitle.text = it?.name
        }
        mCCTVViewModel.cctvModel.observe(
            viewLifecycleOwner
        ) {
            binding.tvTitleSub.text = it?.address
        }
        mCCTVViewModel.closedRangeCalendar.observe(
            viewLifecycleOwner
        ) { calendar ->
            val hasArchive = !calendar.peekContent().isEmpty()
            binding.viewPager.scrollEnabled = hasArchive

            val tab = binding.tabLayout.getTabAt(1) ?: return@observe

            tab.view.isEnabled = hasArchive
            val textRes = if (hasArchive) {
                R.string.cctv_detail_tab_archive
            } else {
                R.string.cctv_detail_tab_archive_is_missing
            }
            tab.text = getString(textRes)
        }
    }

    override fun onPause() {
        super.onPause()

        Timber.d("debug_dmm onPause")
    }

    override fun onStop() {
        super.onStop()

        Timber.d("debug_dmm onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
