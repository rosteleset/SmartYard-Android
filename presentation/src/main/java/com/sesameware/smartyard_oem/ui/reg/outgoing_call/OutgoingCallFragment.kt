package com.sesameware.smartyard_oem.ui.reg.outgoing_call

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.Secure
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sesameware.domain.model.response.UserName
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentOutgoingCallBinding
import com.sesameware.smartyard_oem.ui.reg.sms.SmsRegFragment
import kotlinx.coroutines.Job
import org.koin.androidx.viewmodel.ext.android.viewModel

class OutgoingCallFragment : Fragment() {
    private var _binding: FragmentOutgoingCallBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModel<OutgoingCallViewModel>()
    private var phoneNumber: String = ""
    private var callNumber: String = ""
    private var jobCheckPhone: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    jobCheckPhone?.cancel()
                    this@OutgoingCallFragment.findNavController().navigate(R.id.action_outgoingCallFragment_to_numberRegFragment)
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOutgoingCallBinding.inflate(inflater, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        return binding.root
    }

    @SuppressLint("HardwareIds")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireNotNull(arguments).run {
            phoneNumber = requireNotNull(getString(KEY_PHONE_NUMBER))
            callNumber = requireNotNull(getString(KEY_CALL_NUMBER))
        }
        binding.tvCaption1OC.text = getString(R.string.outgoing_call_caption_1, phoneNumber)
        binding.tvCaptionNumberOC.text = callNumber

        binding.tvChangeNumberOC.setOnClickListener {
            jobCheckPhone?.cancel()
            findNavController().navigate(R.id.action_outgoingCallFragment_to_numberRegFragment)
        }

        binding.ivBack.setOnClickListener {
            jobCheckPhone?.cancel()
            findNavController().navigate(R.id.action_outgoingCallFragment_to_numberRegFragment)
        }

        binding.btnMakeCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$callNumber")
            }
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            }
        }

        val androidId = Secure.getString(requireContext().contentResolver, Secure.ANDROID_ID)
        jobCheckPhone = mViewModel.startRepeatingCheckPhone(androidId, phoneNumber, requireContext())

        mViewModel.phoneConfirmed.observe(viewLifecycleOwner) {
            if (it) {
                jobCheckPhone?.cancel()

                val action = OutgoingCallFragmentDirections
                    .actionOutgoingCallFragmentToAppealFragment()
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroy() {
        jobCheckPhone?.cancel()
        super.onDestroy()
    }

    companion object {
        const val KEY_PHONE_NUMBER = "phone_number"
        const val KEY_CALL_NUMBER = "call_number"
    }
}
