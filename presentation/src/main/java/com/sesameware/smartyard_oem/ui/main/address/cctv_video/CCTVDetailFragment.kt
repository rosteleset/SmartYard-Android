package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCctvDetailBinding
import com.sesameware.smartyard_oem.ui.main.address.addressVerification.TabAdapter
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail.CCTVOnlineTabFragment
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail.arhive.CCTVArchiveTabCalendarFragment
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import timber.log.Timber

class CCTVDetailFragment : Fragment() {
    private var _binding: FragmentCctvDetailBinding? = null
    private val binding get() = _binding!!

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
        setupUi()
        setupObserve()
    }

    private fun setupUi() {
        binding.contentWrap.clipToOutline = true
        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        val adapter = TabAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        Timber.d("debug_dmm __new adapter CCTVDetailFragment")
        adapter.addFragment(
            { CCTVOnlineTabFragment.newInstance() },
            resources.getString(R.string.cctv_detail_tab_online)
        )
        adapter.addFragment(
            { CCTVArchiveTabCalendarFragment.newInstance() },
            resources.getString(R.string.cctv_detail_tab_archive)
        )
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                mCCTVViewModel.setCurrentTabPosition(position)
            }
        })
        binding.viewPager.setCurrentItem(mCCTVViewModel.currentTabId, false)
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
                val onlineTabFragment = (binding.viewPager.adapter as? TabAdapter)?.getItem(CCTVViewModel.ONLINE_TAB_POSITION) as? CCTVOnlineTabFragment
                Timber.d("debug_dmm __onlineTabFragment: $onlineTabFragment")
                Timber.d("__Q__   releasePlayer from onHiddenChanged")
                onlineTabFragment?.releasePlayer()
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            if (mCCTVViewModel.currentTabId == CCTVViewModel.ONLINE_TAB_POSITION) {
                val onlineTabFragment = (binding.viewPager.adapter as? TabAdapter)?.getItem(CCTVViewModel.ONLINE_TAB_POSITION) as? CCTVOnlineTabFragment
                Timber.d("__Q__   initPlayer from onHiddenChanged")
                onlineTabFragment?.initPlayer(mCCTVViewModel.chosenCamera.value?.serverType)
                mCCTVViewModel.chosenCamera.value?.let {cctvData ->
                    onlineTabFragment?.changeVideoSource(cctvData)
                }
            }
        }

        super.onHiddenChanged(hidden)
    }

    override fun onPause() {
        super.onPause()

        Timber.d("debug_dmm onPause")
    }

    override fun onStop() {
        super.onStop()

        Timber.d("debug_dmm onStop")
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.d("debug_dmm onDestroy")
    }
}
