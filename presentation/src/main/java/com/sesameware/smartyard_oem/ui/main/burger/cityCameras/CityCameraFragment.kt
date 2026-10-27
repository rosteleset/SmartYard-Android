@file:Suppress("DEPRECATION")

package com.sesameware.smartyard_oem.ui.main.burger.cityCameras

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.data.DataModule
import com.sesameware.domain.model.response.ProviderConfig
import com.sesameware.lib.dpToPx
import com.sesameware.smartyard_oem.BuildConfig
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCityCameraBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.common.player.CCTVStreamPlayerManager
import com.sesameware.smartyard_oem.ui.common.player.toStreamSourceConfig
import com.sesameware.smartyard_oem.ui.main.ExitFullscreenListener
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.burger.cityCameras.adapters.CityCameraEventAdapter
import com.sesameware.smartyard_oem.ui.main.player.log.PlayerLogFragment
import com.sesameware.smartyard_oem.ui.showStandardAlert
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import org.koin.java.KoinJavaComponent.injectOrNull

class CityCameraFragment : Fragment(), ExitFullscreenListener {
    private var _binding: FragmentCityCameraBinding? = null
    private val binding get() = _binding!!
    private val delegate: CityCameraDelegate? by injectOrNull(CityCameraDelegate::class.java)

    private val playerManager: CCTVStreamPlayerManager by inject()
    private val viewModel: CityCamerasViewModel by sharedStateViewModel()

    //для полноэкранного режима
    private var lpVideoWrap: ViewGroup.LayoutParams? = null
    private var videoWrapAspectRatio = 0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        (activity as? MainActivity)?.setExitFullscreenListener(this)
        _binding = FragmentCityCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindViews()
        setupObservers()
        delegate?.extendConfig(binding, viewModel, viewLifecycleOwner)
    }

    private fun bindViews() {
        binding.ivCityCameraBack.setOnClickListener {
            this.findNavController().popBackStack()
        }

        binding.llCityCameraMain.applyBottomNavInsetsToPadding()

        binding.btnCityCameraEvents.isVisible =
            DataModule.providerConfig.issuesVersion.lowercase() != ProviderConfig.ISSUES_VERSION_NONE
        binding.btnCityCameraEvents.setOnClickListener {
            binding.btnCityCameraEvents.isVisible = false
            binding.clEvents.isVisible = true
        }

        binding.btnCityCameraRequestRecord.setOnClickListener {
            this.findNavController().navigate(R.id.action_cityCameraFragment_to_requestRecordFragment)
        }

        binding.ivCityCameraFullscreen.setOnClickListener {
            viewModel.isFullscreen.value?.let {
                viewModel.setFullscreen(!it)
            }
        }

        binding.videoWrap.clipToOutline = true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        binding.cctvPlayerView.resetZoom()
    }

    override fun onExitFullscreen() {
        if (viewModel.isFullscreen.value == true) {
            viewModel.setFullscreen(false)
        }
    }

    private fun setupObservers() {
        viewModel.chosenCamera.observe(
            viewLifecycleOwner
        ) { camera ->
            camera ?: return@observe

            val slash = camera.name.indexOf("/")
            if (0 < slash && slash < camera.name.length - 1) {
                binding.tvCityCameraTitle.text = camera.name.substring(0, slash).trim()
                binding.tvCityCameraTitleSub.text = camera.name.substring(slash + 1).trim()
            } else {
                binding.tvCityCameraTitle.text = camera.name
                binding.tvCityCameraTitleSub.text = ""
            }

            playerManager.attach(
                lifecycleOwner = viewLifecycleOwner,
                playerView = binding.cctvPlayerView,
                config = camera.toStreamSourceConfig(),
                muteButton = binding.mMute,
                playerLogButton = binding.ivPlayerLog,
                fragmentManager = childFragmentManager
            )
            binding.ivPlayerLog.isVisible = BuildConfig.DEBUG && viewModel.isFullscreen.value == true

            viewModel.getEvents(camera.id) {
                if (viewModel.eventList.isEmpty()) {
                    binding.btnCityCameraEvents.isVisible = false
                    binding.clEvents.isVisible = true
                } else {
                    binding.btnCityCameraEvents.text = resources.getString(
                        R.string.city_camera_events_count,
                        viewModel.eventList.size
                    )
                }
                setupEventAdapter()
            }
        }

        viewModel.isFullscreen.observe(
            viewLifecycleOwner
        ) { fullscreen ->
            if (fullscreen) {
                setFullscreenMode()
            } else {
                setNormalMode()
            }
        }
    }

    private fun setupEventAdapter() {
        val llm = LinearLayoutManager(requireContext())
        binding.rvCityCameraEvents.layoutManager = llm
        val adapter = CityCameraEventAdapter(viewModel.eventList, CityCamerasViewModel.CHUNK_ITEM_COUNT) {
            val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.eventList[it].url))
            try {
                startActivity(youtubeIntent)
            } catch (e: ActivityNotFoundException) {
                showStandardAlert(requireContext(),
                    resources.getString(R.string.error),
                    resources.getString(R.string.city_camera_youtube_msg))
            }
        }
        binding.rvCityCameraEvents.adapter = adapter
        binding.rvCityCameraEvents.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val i1 = llm.findFirstCompletelyVisibleItemPosition()
                val i2 = llm.findLastCompletelyVisibleItemPosition()

                if (i2 == viewModel.eventList.size - 1 && i1 > 0) {
                    binding.ivCityCameraArrowUp.visibility = View.VISIBLE
                    binding.tvCityCameraScroll.text = resources.getString(R.string.city_camera_scroll_up)
                    binding.llCityCameraBottom.visibility = View.VISIBLE
                } else {
                    if (i2 == adapter.currentSize() - 1 && adapter.currentSize() < viewModel.eventList.size) {
                        binding.ivCityCameraArrowUp.visibility = View.INVISIBLE
                        val diff = viewModel.eventList.size - adapter.currentSize()
                        val more =
                            if (diff < CityCamerasViewModel.CHUNK_ITEM_COUNT) diff else CityCamerasViewModel.CHUNK_ITEM_COUNT
                        binding.tvCityCameraScroll.text =
                            resources.getString(R.string.city_camera_load_more, more)
                        binding.llCityCameraBottom.visibility = View.VISIBLE
                    } else {
                        binding.llCityCameraBottom.visibility = View.INVISIBLE
                    }
                }
            }
        })

        binding.llCityCameraBottom.setOnClickListener {
            if (adapter.currentSize() < viewModel.eventList.size) {
                adapter.setCurrentSize(adapter.currentSize() + CityCamerasViewModel.CHUNK_ITEM_COUNT)
            } else {
                llm.scrollToPosition(0)
            }
            binding.llCityCameraBottom.visibility = View.INVISIBLE
        }
    }

    private fun setFullscreenMode() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //сохраняем дефолтный layout
        lpVideoWrap = binding.videoWrap.layoutParams

        binding.ivPlayerLog.isVisible = BuildConfig.DEBUG
        binding.ivCityCameraFullscreen.isSelected = true
        binding.videoWrap.background = null
        videoWrapAspectRatio = binding.videoWrap.aspectRatio
        binding.videoWrap.setAspectRatio(0f)
        (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(Color.BLACK)

        (activity as? MainActivity)?.hideSystemUI()
        (activity as? MainActivity)?.binding?.navHostContainer?.visibility = View.INVISIBLE
        (binding.videoWrap.parent as ViewGroup).removeView(binding.videoWrap)
        (activity as? MainActivity)?.binding?.llMain?.addView(binding.videoWrap, 0)

        binding.videoWrap.applyBottomNavInsetsToPadding()
        val lp = binding.videoWrap.layoutParams as FrameLayout.LayoutParams
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT
        lp.topMargin = 0
        binding.videoWrap.layoutParams = lp
        binding.videoWrap.requestLayout()
        binding.cctvPlayerView.resetZoom()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setNormalMode() {
        val logFragment = childFragmentManager.findFragmentByTag(PlayerLogFragment.TAG)
        if (logFragment != null && logFragment.isAdded && !logFragment.isHidden) {
            childFragmentManager.beginTransaction().hide(logFragment).commit()
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        (activity as? MainActivity)?.showSystemUI()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        (binding.videoWrap.parent as ViewGroup).removeView(binding.videoWrap)
        if (videoWrapAspectRatio > 0f) binding.videoWrap.setAspectRatio(videoWrapAspectRatio)
        (activity as? MainActivity)?.binding?.navHostContainer?.visibility = View.VISIBLE
        binding.llCityCameraMain.addView(binding.videoWrap, 0)
        binding.ivPlayerLog.isVisible = false
        binding.ivCityCameraFullscreen.isSelected = false
        binding.videoWrap.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_radius_video_clip)

        //возвращаем дефолтные layouts
        lpVideoWrap?.let { lp ->
            ViewCompat.setOnApplyWindowInsetsListener(binding.videoWrap, null)
            if (lp is ViewGroup.MarginLayoutParams) {
                lp.height = 222.dpToPx()
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.marginStart = 16.dpToPx()
                lp.marginEnd = 16.dpToPx()
                lp.topMargin = 16.dpToPx()
            }
            binding.videoWrap.layoutParams = lp
            binding.videoWrap.requestLayout()
        }
        binding.cctvPlayerView.resetZoom()
        (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.shaded_background))
    }

    override fun onDestroyView() {
        (activity as? MainActivity)?.setExitFullscreenListener(null)
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        _binding = null

        super.onDestroyView()
    }
}
