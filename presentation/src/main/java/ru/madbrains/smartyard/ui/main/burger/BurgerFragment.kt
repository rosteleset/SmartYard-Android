package ru.madbrains.smartyard.ui.main.burger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_burger.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.showStandardAlert

class BurgerFragment : Fragment() {
    private val viewModel: BurgerViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_burger, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cvBurgerCityCameras.setOnClickListener {
            this.findNavController().navigate(R.id.action_burgerFragment_to_cityCamerasFragment)
        }

        cvBurgerAddressSettings.setOnClickListener {
            this.findNavController().navigate(R.id.action_burgerFragment_to_settingsFragment)
        }

        cvBurgerCommonSettings.setOnClickListener {
            this.findNavController().navigate(R.id.action_burgerFragment_to_basicSettingsFragment)
        }

        llCallSupport.setOnClickListener {
            viewModel.getHelpMe()
            val dialog = CallToSupportFragment()
            dialog.show(requireActivity().supportFragmentManager, "callToSupport")
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.chosenSupportOption.observe(
            viewLifecycleOwner,
            {
                it?.let { supportOption ->
                    when(supportOption) {
                        BurgerViewModel.SupportOption.CALL_TO_SUPPORT_BY_PHONE -> callToSupportByPhone(viewModel.dialNumber.value ?: "")
                        BurgerViewModel.SupportOption.ORDER_CALLBACK -> orderCallback()
                    }
                }
            }
        )

        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                showStandardAlert(requireContext(), R.string.issue_dialog_caption_0) {
                    this.findNavController().popBackStack()
                }
            }
        )
    }

    private fun callToSupportByPhone(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun orderCallback() {
        viewModel.createIssue()
    }
}