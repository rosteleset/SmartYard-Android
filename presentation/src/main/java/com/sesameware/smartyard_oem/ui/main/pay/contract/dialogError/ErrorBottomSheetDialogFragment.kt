package com.sesameware.smartyard_oem.ui.main.pay.contract.dialogError

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentErrorBottomSheetDialogBinding

class ErrorBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentErrorBottomSheetDialogBinding? = null
    private val binding get() = _binding!!

    private val args: ErrorBottomSheetDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErrorBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvError.text = args.textError
    }
}
