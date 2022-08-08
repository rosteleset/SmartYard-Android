package ru.madbrains.smartyard.ui.reg.providers

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import ru.madbrains.smartyard.databinding.FragmentProvidersBinding

class ProvidersFragment : Fragment() {
    private var _binding: FragmentProvidersBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by viewModels<ProvidersViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProvidersBinding.inflate(inflater, container, false)
        return binding.root
    }
}
