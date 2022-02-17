package ru.madbrains.smartyard.ui.main.pay.contract.dialogError

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentErrorButtomSheetDialogBinding

class ErrorBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentErrorButtomSheetDialogBinding? = null
    private val binding get() = _binding!!

    private val args: ErrorBottomSheetDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErrorButtomSheetDialogBinding.inflate(inflater, container, false)
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
