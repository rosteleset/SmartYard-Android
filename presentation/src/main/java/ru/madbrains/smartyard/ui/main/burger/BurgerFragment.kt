package ru.madbrains.smartyard.ui.main.burger

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentBurgerBinding
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogDetailFragment
import ru.madbrains.smartyard.ui.main.settings.SettingsFragment
import ru.madbrains.smartyard.ui.main.settings.basicSettings.BasicSettingsFragment
import ru.madbrains.smartyard.ui.showStandardAlert
import timber.log.Timber

class BurgerFragment : Fragment() {
    private var _binding: FragmentBurgerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BurgerViewModel by sharedViewModel()

    private lateinit var adapter: ListDelegationAdapter<List<BurgerModel>>

    private var click = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBurgerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.llCallSupport.setOnClickListener {
            viewModel.getHelpMe()
            val dialog = CallToSupportFragment()
            dialog.show(requireActivity().supportFragmentManager, "callToSupport")
        }

        binding.textView5.setOnClickListener {
            click += 1
            if (click == 10) {
                val videoId = "dQw4w9WgXcQ"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
                startActivity(intent)
                click = 0
            }
        }

        binding.tvPolicy.setOnClickListener {
            val url = resources.getString(R.string.privacy_policy)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            }
        }

        initRecycler()
        setupObservers()
    }

    private fun initRecycler() {
        binding.rvBurger.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        adapter = ListDelegationAdapter(
            BurgerDelegate()
        )
        binding.rvBurger.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.chosenSupportOption.observe(
            viewLifecycleOwner
        ) {
            it?.let { supportOption ->
                when (supportOption) {
                    BurgerViewModel.SupportOption.CALL_TO_SUPPORT_BY_PHONE -> callToSupportByPhone(
                        viewModel.dialNumber.value ?: ""
                    )

                    BurgerViewModel.SupportOption.ORDER_CALLBACK -> orderCallback()
                }
            }
        }

        viewModel.navigateToIssueSuccessDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                showStandardAlert(requireContext(), R.string.issue_dialog_caption_0) {
                    this.findNavController().popBackStack()
                }
            }
        )

        viewModel.burgerList.observe(
            viewLifecycleOwner
        ) {
            adapter.items = it
            adapter.notifyDataSetChanged()
        }

        viewModel.navigateToFragment.observe(
            viewLifecycleOwner,
            EventObserver {
                if (it == R.id.action_burgerFragment_to_settingsFragment){
                    val transaction = fragmentManager?.beginTransaction()
                    transaction?.add(R.id.cl_bureger_fragment, SettingsFragment())
                    transaction?.addToBackStack(null)
                    transaction?.commit()
                }
                if (it == R.id.action_burgerFragment_to_basicSettingsFragment){
                    val transaction = fragmentManager?.beginTransaction()
                    transaction?.add(R.id.cl_bureger_fragment, BasicSettingsFragment())
                    transaction?.addToBackStack(null)
                    transaction?.commit()
                }
//                this.findNavController().navigate(it)
            }
        )

        viewModel.navigateToWebView.observe(
            viewLifecycleOwner,
            EventObserver {
                val action = BurgerFragmentDirections.actionBurgerFragmentToExtWebViewFragment(
                    it.basePath,
                    it.code
                )
                this.findNavController().navigate(action)
            }
        )
    }

    private fun callToSupportByPhone(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        try {
            startActivity(intent)
        }catch (_: ActivityNotFoundException){}
    }

    private fun orderCallback() {
        viewModel.createIssue()
    }
}