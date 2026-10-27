package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentEntranceCameraBinding
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.common.player.CCTVStreamPlayerManager
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.AddressViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class EntranceCameraFragment : Fragment() {
    private var _binding: FragmentEntranceCameraBinding? = null
    private val binding get() = _binding!!

    private val args: EntranceCameraFragmentArgs by navArgs()
    private val viewModel by sharedViewModel<AddressViewModel>()
    private val playerManager: CCTVStreamPlayerManager by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntranceCameraBinding.inflate(inflater, container, false)
        binding.root.applyBottomNavInsetsToPadding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterFullscreen()
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC
        bindViews()

        playerManager.attach(
            lifecycleOwner = viewLifecycleOwner,
            playerView = binding.cctvPlayerView,
            config = args.cameraConfig,
            muteButton = binding.muteWrapper,
            playerLogButton = binding.ivPlayerLog,
            fragmentManager = childFragmentManager
        )
    }

    private fun bindViews() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.tbOpen.setOnClickListener {
            Timber.d(
                "%s%s",
                "debug_dmm Opening door: domophoneId=${args.lock.domophoneId}, ",
                "doorId=${args.lock.doorId}"
            )
            runCatching { viewModel.openDoor(args.lock) }
            _binding?.tbOpen?.isClickable = false
            _binding?.root?.postDelayed(
                {
                    _binding?.tbOpen?.isChecked = false
                    _binding?.tbOpen?.isClickable = true
                },
                3000
            )
        }
    }

    private fun enterFullscreen() {
        (activity as? MainActivity)?.hideSystemUI()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun exitFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        (activity as? MainActivity)?.showSystemUI()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroyView() {
        exitFullscreen()
        _binding = null
        super.onDestroyView()
    }
}
