package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.threeten.bp.LocalDate
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCctvDetailBinding
import com.sesameware.smartyard_oem.ui.main.address.addressVerification.TabAdapter
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail.CCTVOnlineTab
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail.arhive.CCTVArchiveTab
import timber.log.Timber

class CCTVDetailFragment : Fragment() {
    private var _binding: FragmentCctvDetailBinding? = null
    private val binding get() = _binding!!

    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Timber.d("debug_dmm __onActivityCreated")
        mCCTVViewModel.closedRangeCalendar.observe(
            viewLifecycleOwner,
            EventObserver {
                mCCTVViewModel.startDate = it.start
                mCCTVViewModel.endDate = it.endInclusive
                setupUi(childFragmentManager)
            }
        )
        setupUi(childFragmentManager)
        setupObserve()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCctvDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("debug_dmm onViewCreated")
    }

    private fun setupUi(fm: FragmentManager
    ) {
        binding.contentWrap.clipToOutline = true
        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        val adapter = TabAdapter(fm)
        Timber.d("debug_dmm __new adapter CCTVDetailFragment")
        adapter.addFragment(
            CCTVOnlineTab.newInstance(),
            resources.getString(R.string.cctv_detail_tab_online)
        )

        adapter.addFragment(
            CCTVArchiveTab.newInstance(mCCTVViewModel.startDate, mCCTVViewModel.endDate, mCCTVViewModel.availableRanges),
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
    }

    private fun setupTabs(adapter: TabAdapter, position: Int) {
        val onlineTabFragment =
            adapter.getItem(CCTVViewModel.ONLINE_TAB_POSITION) as CCTVOnlineTab
        when (position) {
            CCTVViewModel.ARCHIVE_TAB_POSITION -> {
                onlineTabFragment.releasePlayer()
            }
            CCTVViewModel.ONLINE_TAB_POSITION -> {
                onlineTabFragment.initPlayer()
            }
        }
    }

    fun archiveCallback(chosenDate: LocalDate) {
        val action =
            CCTVDetailFragmentDirections.actionCCTVDetailFragmentToCCTVTrimmerFragment(
                chosenDate,
                mCCTVViewModel.startDate
            )

        this.findNavController().navigate(action)
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
    }

    override fun onHiddenChanged(hidden: Boolean) {
        Timber.d("debug_dmm __detail fragment onHidden, hidden = $hidden")
        if (hidden) {
            if (mCCTVViewModel.currentTabId == CCTVViewModel.ONLINE_TAB_POSITION) {
                val onlineTabFragment = (binding.viewPager.adapter as? TabAdapter)?.getItem(CCTVViewModel.ONLINE_TAB_POSITION) as? CCTVOnlineTab
                Timber.d("debug_dmm __onlineTabFragment: $onlineTabFragment")
                onlineTabFragment?.releasePlayer()
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            if (mCCTVViewModel.currentTabId == CCTVViewModel.ONLINE_TAB_POSITION) {
                val onlineTabFragment = (binding.viewPager.adapter as? TabAdapter)?.getItem(CCTVViewModel.ONLINE_TAB_POSITION) as? CCTVOnlineTab
                onlineTabFragment?.initPlayer()
            }
        }

        super.onHiddenChanged(hidden)
    }
}
