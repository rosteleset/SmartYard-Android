package com.sesameware.smartyard_oem.ui.main.settings.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sesameware.data.prefs.NightMode
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.BottomSheetFragmentSelectThemeBinding
import com.sesameware.smartyard_oem.ui.main.settings.basicSettings.BasicSettingsFragment

class SelectThemeBottomSheetFragment : BottomSheetDialogFragment(), View.OnClickListener {

    private var _binding: BottomSheetFragmentSelectThemeBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFragmentSelectThemeBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.let {
//            it.root.parent.setBackgroundDrawable(ColorDrawable(Color.parseColor("#00000000")))
            it.tvItemSystemDefault.setOnClickListener(this)
            it.tvItemLight.setOnClickListener(this)
            it.tvItemDark.setOnClickListener(this)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(v: View?) {
        val mode = when (v?.id) {
            R.id.tvItemSystemDefault -> NightMode.FOLLOW_SYSTEM
            R.id.tvItemLight -> NightMode.NO
            R.id.tvItemDark -> NightMode.YES
            else -> return
        }
        val bundle = Bundle().apply {
            putParcelable(BasicSettingsFragment.NIGHT_MODE_VALUE, mode)
        }
        setFragmentResult(BasicSettingsFragment.REQUEST_NIGHT_MODE, bundle)
        dismiss()
    }
}