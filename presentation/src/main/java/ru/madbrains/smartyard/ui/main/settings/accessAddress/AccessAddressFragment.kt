package ru.madbrains.smartyard.ui.main.settings.accessAddress

import android.Manifest
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentAccessAddressBinding
import ru.madbrains.smartyard.ui.Type
import ru.madbrains.smartyard.ui.main.settings.accessAddress.adapterdelegates.ContactAdapterDelegate
import ru.madbrains.smartyard.ui.main.settings.accessAddress.dialogShareAccess.DialogShareAccessDialog
import ru.madbrains.smartyard.ui.main.settings.accessAddress.models.ContactModel
import ru.madbrains.smartyard.ui.showStandardAlert
import ru.madbrains.smartyard.ui.webview_dialog.WebViewDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar

class AccessAddressFragment : Fragment() {
    private var _binding: FragmentAccessAddressBinding? = null
    private val binding get() = _binding!!

    private var listAccessBarrierGate = mutableListOf<ContactModel>()
    private var listPermanentAccessAddress = mutableListOf<ContactModel>()
    private lateinit var adapterAccessBarrierGate: ListDelegationAdapter<List<ContactModel>>
    private lateinit var adapterPermanentAccessAddress: ListDelegationAdapter<List<ContactModel>>

    private val mViewModel by viewModel<AccessAddressViewModel>()

    private var flatId: Int = 0
    private var address: String = ""
    private var contractOwner: Boolean = false
    private var hasGates: Boolean = false
    private var clientId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccessAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initAddContact() {
        binding.tvAddBarrierGate.setOnClickListener {
            val dialog = DialogShareAccessDialog()
            dialog.onDialogServiceListener =
                object : DialogShareAccessDialog.OnDialogAccessListener {
                    override fun onDone(contactModel: ContactModel) {
                        mViewModel.addRoommate(flatId, contactModel.number, "outer")
                        dialog.dismiss()
                    }
                }
            dialog.show(parentFragmentManager, "")
        }

        binding.tvPermanentAccessAddress.setOnClickListener {
            val dialog = DialogShareAccessDialog()
            dialog.onDialogServiceListener =
                object : DialogShareAccessDialog.OnDialogAccessListener {
                    override fun onDone(contactModel: ContactModel) {
                        mViewModel.addRoommate(flatId, contactModel.number, "inner")
                        dialog.dismiss()
                    }
                }
            dialog.show(parentFragmentManager, "")
        }
    }

    private fun initObservable() {
        mViewModel.intercom.observe(
            viewLifecycleOwner,
            Observer {
                if (it.doorCode == null) {
                    binding.llCode.isVisible = false
                } else {
                    binding.tvCodeOpen.text = it.doorCode
                }
                val c: Calendar = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val getCurrentDateTime = sdf.format(c.time)
                if (getCurrentDateTime <= it.autoOpen) {
                    binding.btnGuestAccessOpen.isClickable = false
                    binding.btnGuestAccessOpen.isChecked = true
                }
                hideCodeOpen(it.allowDoorCode)

                if (it.frsDisabled == false) {
                    binding.expLayoutByFace.expand()
                } else {
                    binding.expLayoutByFace.collapse()
                }
            }
        )

        mViewModel.roommate.observe(
            viewLifecycleOwner,
            Observer {
                val inner = it.filter { it.type == Type.INNER.value }
                val outer = it.filter { it.type == Type.OUTER.value }
                adapterAccessBarrierGate.items =
                    outer.map { ContactModel(it.expire, it.phone) }.toMutableList()
                adapterAccessBarrierGate.notifyDataSetChanged()

                adapterPermanentAccessAddress.items =
                    inner.map { ContactModel(it.expire, it.phone) }.toMutableList()
                adapterPermanentAccessAddress.notifyDataSetChanged()
            }
        )

        mViewModel.operationRoommate.observe(
            viewLifecycleOwner,
            Observer {
                mViewModel.getRoommateAndIntercom(flatId)
            }
        )

        mViewModel.dialogToSuccessSms.observe(
            viewLifecycleOwner,
            EventObserver {
                showStandardAlert(
                    requireContext(),
                    resources.getString(R.string.dialog_sms),
                    null,
                    true
                )
            }
        )

        mViewModel.resetCode.observe(
            viewLifecycleOwner,
            Observer {
                binding.tvCodeOpen.text = it.code.toString()
            }
        )

        binding.ivRefreshCode.setOnClickListener {
            mViewModel.resetCode(flatId)
        }
    }

    private fun checkHide() {
        // скрываем постоянный доступ к адресу
        binding.cvPermanentAccessAddress.isVisible = contractOwner
        binding.tvTitlePermanentAccessAddress.isVisible = contractOwner

        // Если hasGates = false, то скрываем временный доступ
        binding.tvTitleAccessBarrierGate.isVisible = hasGates
        binding.cvAccessGate.isVisible = hasGates
    }

    private fun hideCodeOpen(availableDoorCode: Boolean) {
        binding.llCode.isVisible = availableDoorCode
        binding.view8.isVisible = availableDoorCode
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            flatId = AccessAddressFragmentArgs.fromBundle(it).flatId
            address = AccessAddressFragmentArgs.fromBundle(it).address
            contractOwner = AccessAddressFragmentArgs.fromBundle(it).contractOwner
            hasGates = AccessAddressFragmentArgs.fromBundle(it).hasGates
            clientId = AccessAddressFragmentArgs.fromBundle(it).clientId
        }

        requestCameraPermission()
        checkHide()
        hideIfPon(address) //TODO Костыль нужно поправить!
        initRecycler()
        initAddContact()
        initObservable()

        mViewModel.getRoommateAndIntercom(flatId)

        binding.tvShareAccess.setOnClickListener {
            WebViewDialogFragment(R.string.help_share_access).show(requireActivity().supportFragmentManager, "HelpShareAccess")
        }

        binding.btnGuestAccessOpen.isChecked = false
        binding.btnGuestAccessOpen.setOnClickListener {
            showDialog()
        }

        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
        binding.tvAddressName.text = address

        binding.btnManageFaces.setOnClickListener {
            val action = AccessAddressFragmentDirections.actionAccessAddressFragmentToFaceSettingsFragment(address)
            action.flatId = flatId
            this.findNavController().navigate(action)
        }
    }

    private fun hideIfPon(address: String){
        val pattern = "квартира".toRegex()
        if (!pattern.containsMatchIn(address)){
            binding.cvCodeAndGuestAccess.isVisible = false
            binding.tvTitleIntecom.isVisible = false
            binding.llFrs.isVisible = false
        }
    }//TODO Костыльный костыль для адресов PON которые не имеют номер кв

    private fun initRecycler() {
        binding.rvAccessBarrierGate.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        adapterAccessBarrierGate = ListDelegationAdapter<List<ContactModel>>(
            ContactAdapterDelegate(
                requireActivity(), false,
                { _, number ->
                    showDialogDelete(number = number)
                },
                { number -> mViewModel.resend(flatId, number) }
            )
        )
        adapterAccessBarrierGate.items = listAccessBarrierGate
        binding.rvAccessBarrierGate.adapter = adapterAccessBarrierGate

        binding.rvPermanentAccessAddress.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        adapterPermanentAccessAddress = ListDelegationAdapter<List<ContactModel>>(
            ContactAdapterDelegate(
                requireActivity(), true,
                { _, number ->
                    showDialogDelete(number = number)
                },
                { number ->
                    mViewModel.resend(flatId, number)
                }
            )
        )

        adapterPermanentAccessAddress.items = listPermanentAccessAddress
        binding.rvPermanentAccessAddress.adapter = adapterPermanentAccessAddress
    }

    private fun showDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        builder
            .setTitle(resources.getString(R.string.dialog_title))
            .setMessage(resources.getString(R.string.dialog_message))
            .setPositiveButton(resources.getString(R.string.dialog_yes)) { _, _ ->
                mViewModel.guestAccessOpen(flatId)
                binding.btnGuestAccessOpen.isClickable = false
            }
            .setNegativeButton(resources.getString(R.string.dialog_no)) { _, _ ->
                binding.btnGuestAccessOpen.isChecked = false
                returnTransition
            }.show()
    }

    private fun requestCameraPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
            0
        )
    }

    private fun showDialogDelete(number: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        builder
            .setMessage(resources.getString(R.string.setting_dialog_delete_title))
            .setPositiveButton(resources.getString(R.string.setting_dialog_delete_yes)) { _, _ ->
                mViewModel.deleteRooomate(flatId, number, clientId)
            }
            .setNegativeButton(resources.getString(R.string.setting_dialog_delete_no)) { _, _ ->
                returnTransition
            }.show()
    }
}
