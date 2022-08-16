package ru.madbrains.smartyard.ui.reg.auth_types

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.databinding.FragmentAuthTypesBinding
import ru.madbrains.smartyard.ui.reg.providers.ProvidersViewModel

class AuthTypesFragment : Fragment() {
    private var _binding: FragmentAuthTypesBinding? = null
    private val binding get() = _binding!!

    private val mViewModel by sharedViewModel<AuthTypesViewModel>()
    private val providersVM by sharedViewModel<ProvidersViewModel>()
    private lateinit var adapter: AuthTypesAdapter
    private var methodId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthTypesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecycler()

        providersVM.authTypesList.observe(viewLifecycleOwner) {
            it?.let { authList ->
                adapter.items = authList.map { item ->
                    AuthTypesModel(item.methodId ?: "", item.name ?: "", false)
                }
                adapter.notifyDataSetChanged()
                binding.btnChooseAuthType.isEnabled = false
                methodId = ""
            }
        }

        binding.btnChooseAuthType.setOnClickListener {
            mViewModel.goToNext(this, methodId)
        }
    }

    private fun initRecycler() {
        activity?.let {
            binding.rvAuthTypes.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = AuthTypesAdapter(it) { id ->
                methodId = id
                binding.btnChooseAuthType.isEnabled = methodId.isNotEmpty()
            }
            binding.rvAuthTypes.adapter = adapter
            binding.rvAuthTypes.addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
        }
    }
}
