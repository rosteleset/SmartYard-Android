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
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCctvDetailBinding
import com.sesameware.smartyard_oem.ui.main.address.addressVerification.TabAdapter
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail.CCTVOnlineTabFragment
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail.arhive.CCTVArchiveTabCalendarFragment
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
        setupUi(childFragmentManager)
        setupObserve()
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
