package com.sesameware.smartyard_oem.ui.reg.appeal

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentAppealBinding
import com.sesameware.smartyard_oem.ui.requestPermission

class AppealFragment : Fragment() {
    private var _binding: FragmentAppealBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppealBinding.inflate(inflater, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                0,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }

        (requireActivity() as CommonActivity).lightNavBar = true

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    //игнорируем навигацию назад
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.mForm.initialize(
            viewLifecycleOwner,
            R.string.next,
            true
        ) {
            this.findNavController().navigate(R.id.action_appealFragment_to_mainActivity)
            val permissions = arrayListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            requestPermission(
                permissions,
                requireContext(),
                onGranted = {
                },
                onDenied = {
                }
            )
            activity?.finish()
        }
    }
}
