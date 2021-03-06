package ru.madbrains.smartyard.ui.reg.appeal

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentAppealBinding
import ru.madbrains.smartyard.ui.requestPermission

class AppealFragment : Fragment() {
    private var _binding: FragmentAppealBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.mForm.initialize(viewLifecycleOwner, R.string.next, arguments) {
            this.findNavController().navigate(R.id.action_appealFragment_to_mainActivity)
            requestPermission(
                arrayListOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
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
