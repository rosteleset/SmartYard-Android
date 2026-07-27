package com.sesameware.smartyard_oem.ui.main.settings.accessAddress

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.tabs.TabLayout
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import com.sesameware.data.DataModule
import com.sesameware.domain.model.response.GuestAccessType
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentAccessAddressBinding
import com.sesameware.smartyard_oem.ui.Type
import com.sesameware.smartyard_oem.ui.applyBottomNavInsetsToPadding
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter.ItemGateAccessByLicensePlateBinder
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter.ItemGateAccessByLicensePlateShowAllBinder
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter.ItemGateAccessByPhoneBinder
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter.ItemGateAccessByPhoneShowAllBinder
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapterdelegates.ContactAdapterDelegate
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogShareAccess.DialogShareAccessDialog
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.manager.FixedListItemManager
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.ContactModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.models.LicensePlateValue
import com.sesameware.smartyard_oem.ui.showStandardAlert
import com.sesameware.smartyard_oem.ui.webview_dialog.WebViewDialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AccessAddressFragment : Fragment() {
    private var _binding: FragmentAccessAddressBinding? = null
    private val binding get() = _binding!!

    private var gateAccessByPhoneManager: FixedListItemManager<ContactModel>? = null
    private var gateAccessByLicensePlateManager: FixedListItemManager<LicensePlateValue>? = null
    private var permanentAccessAdapter: AsyncListDifferDelegationAdapter<ContactModel>? = null

    private val mViewModel by sharedViewModel<AccessAddressViewModel>()

    private var flatId: Int = 0
    private var address: String = ""
    private var flatOwner: Boolean = false
    private var hasGates: Boolean = false
    private var hasPlog: Boolean = false
    private var hasLprs: Boolean = false
    private var clientId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccessAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            flatId = AccessAddressFragmentArgs.fromBundle(it).flatId
            address = AccessAddressFragmentArgs.fromBundle(it).address
            flatOwner = AccessAddressFragmentArgs.fromBundle(it).flatOwner
            hasGates = AccessAddressFragmentArgs.fromBundle(it).hasGates
            hasPlog = AccessAddressFragmentArgs.fromBundle(it).hasPlog
            clientId = AccessAddressFragmentArgs.fromBundle(it).clientId
        }

        requestCameraPermission()

        binding.scrollView.applyBottomNavInsetsToPadding()
        initGateAccess()
        initPermanentAddressAccess()
        initAddContact()
        initObservable()

        mViewModel.getRoommateAndIntercom(flatId)

        binding.tvShareAccess.setOnClickListener {
            WebViewDialogFragment(R.string.help_share_access)
                .show(requireActivity().supportFragmentManager, "HelpShareAccess")
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
            val action = AccessAddressFragmentDirections
                .actionAccessAddressFragmentToFaceSettingsFragment(address)
            action.flatId = flatId
            action.canAddFace = hasPlog
            this.findNavController().navigate(action)
        }
    }

    private fun initAddContact() {
        binding.tvAddGateAccess.setOnClickListener {
            when {
                !hasLprs && !hasGates -> return@setOnClickListener
                hasLprs && !hasGates -> createDialogShareAccessDialog(AccessType.GATE_BY_LICENSE_PLATE)
                !hasLprs && hasGates -> createDialogShareAccessDialog(AccessType.GATE_BY_PHONE)
                else -> {
                    if (binding.tlGateAccess.selectedTabPosition == 0) {
                        createDialogShareAccessDialog(AccessType.GATE_BY_LICENSE_PLATE)
                    } else {
                        createDialogShareAccessDialog(AccessType.GATE_BY_PHONE)
                    }
                }
            }
        }

        binding.tvAddPermanentAccessAddress.setOnClickListener {
            createDialogShareAccessDialog(AccessType.ADDRESS_PERMANENT)
        }
    }

    private fun createDialogShareAccessDialog(type: AccessType) {
        DialogShareAccessDialog(type, ::onAddAccessByPhone, ::onAddAccessByLicensePlate)
            .show(parentFragmentManager, "")
    }

    private fun onAddAccessByPhone(contact: ContactModel, type: AccessType) {
        mViewModel.addRoommate(flatId, contact.number, type.roommateType)
    }

    private fun onAddAccessByLicensePlate(plate: LicensePlateValue) {
        mViewModel.addLicensePlate(flatId, plate)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initObservable() {
        mViewModel.intercom.observe(
            viewLifecycleOwner
        ) {
            it?.let {
                if (it.doorCode == null) {
                    binding.gOpeningCode.isVisible = false
                } else {
                    binding.tvOpeningCode.text = it.doorCode
                }
                val c: Calendar = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val getCurrentDateTime = sdf.format(c.time)
                if (DataModule.providerConfig.guestAccess == GuestAccessType.TURN_ON_ONLY) {
                    if (getCurrentDateTime <= it.autoOpen) {
                        binding.btnGuestAccessOpen.isClickable = false
                        binding.btnGuestAccessOpen.isChecked = true
                    }
                } else {
                    binding.btnGuestAccessOpen.isClickable = true
                    binding.btnGuestAccessOpen.isChecked = (getCurrentDateTime <= it.autoOpen)
                }
                hideCodeOpen(it.allowDoorCode)

                binding.gEnterByFace.isVisible = it.frsDisabled == false

                hasLprs = it.lprsDisabled == false
                with (binding) {
                    tvTitleGateAccess.isVisible = hasLprs || hasGates
                    tvAddGateAccess.isVisible = hasLprs || hasGates
                    tlGateAccess.isVisible = hasLprs && hasGates

                    if (hasLprs && hasGates) {
                        val isPlateTabSelected = tlGateAccess.selectedTabPosition == 0
                        hsvGateAccessByLicensePlate.root.visibility =
                            if (isPlateTabSelected) View.VISIBLE else View.INVISIBLE
                        hsvGateAccessByPhone.root.visibility =
                            if (!isPlateTabSelected) View.VISIBLE else View.INVISIBLE
                    } else {
                        hsvGateAccessByLicensePlate.root.isVisible = hasLprs
                        hsvGateAccessByPhone.root.isVisible = hasGates
                    }
                }

                binding.gPermanentAccessAddress.isVisible = flatOwner
            }
        }

        mViewModel.roommate.observe(
            viewLifecycleOwner
        ) { roommateList ->
            val map = roommateList
                .groupBy(
                    keySelector = { it.type == Type.OUTER.value },
                    valueTransform = {
                        ContactModel(it.expire, it.phone,
                            it.type == Type.OWNER.value)
                    }
                )
            val gateByPhoneList = map[true] ?: listOf<ContactModel>()
            val permanentAccessList = map[false] ?: listOf<ContactModel>()

            val stub = listOf(ContactModel("", ""))
            gateAccessByPhoneManager!!.submitList(
                gateByPhoneList.take(CARD_ITEMS_FOR_PREVIEW), stub
            )

            permanentAccessAdapter!!.items = permanentAccessList
        }

        mViewModel.licensePlates.observe(
            viewLifecycleOwner
        ) { lpList ->
            val stub = listOf(LicensePlateValue(""))
            gateAccessByLicensePlateManager!!.submitList(
                lpList.take(CARD_ITEMS_FOR_PREVIEW).map { LicensePlateValue(it.value) }, stub
            )
        }

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
            viewLifecycleOwner
        ) {
            it?.let {
                binding.tvOpeningCode.text = it.code.toString()
            }
        }

        binding.ivRefreshCode.setOnClickListener {
            mViewModel.resetCode(flatId)
        }
    }

    private fun hideCodeOpen(availableDoorCode: Boolean) {
        binding.gOpeningCode.isVisible = availableDoorCode
    }

    private fun initGateAccess() {
        binding.hsvGateAccessByPhone.root.isVisible = false
        binding.tlGateAccess.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    with (binding) {
                        when (tab.position) {
                            0 -> {
                                hsvGateAccessByLicensePlate.root.visibility = View.VISIBLE
                                hsvGateAccessByPhone.root.visibility = View.INVISIBLE
                            }
                            1 -> {
                                hsvGateAccessByLicensePlate.root.visibility = View.INVISIBLE
                                hsvGateAccessByPhone.root.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            }
        )
        gateAccessByPhoneManager = FixedListItemManager<ContactModel>(
            binding.hsvGateAccessByPhone.llGateAccess,
            ItemGateAccessByPhoneBinder(
                CARD_ITEMS_FOR_PREVIEW,
                ::onGateAccessByPhoneItemClick,
                ::onGateAccessByPhoneMenuClick
            ),
            ItemGateAccessByPhoneShowAllBinder(
                SHOW_ALL_ITEMS_FOR_PREVIEW,
                ::onGateAccessByPhoneShowAllItemClick
            ),
        )
        gateAccessByLicensePlateManager = FixedListItemManager<LicensePlateValue>(
            binding.hsvGateAccessByLicensePlate.llGateAccess,
            ItemGateAccessByLicensePlateBinder(
                CARD_ITEMS_FOR_PREVIEW,
                ::onGateAccessByLicensePlateItemClick,
                ::onGateAccessByLicensePlateMenuClick
            ),
            ItemGateAccessByLicensePlateShowAllBinder(
                SHOW_ALL_ITEMS_FOR_PREVIEW,
                ::onGateAccessByLicensePlateShowAllItemClick
            ),
        )
    }

    private fun onGateAccessByPhoneItemClick(position: Int) {
        navigateToDetail(flatId, address, 1, position, clientId)
    }

    private fun onGateAccessByPhoneShowAllItemClick() {
        onGateAccessByPhoneItemClick(0)
    }

    private fun onGateAccessByLicensePlateItemClick(position: Int) {
        navigateToDetail(flatId, address, 0, position, clientId)
    }

    private fun onGateAccessByLicensePlateShowAllItemClick() {
        onGateAccessByLicensePlateItemClick(0)
    }

    private fun navigateToDetail(
        flatId: Int,
        address: String,
        initialTab: Int,
        initialPosition:Int,
        clientId: String
    ) {
        val action = AccessAddressFragmentDirections
            .actionAccessAddressFragmentToDetailGateAccessFragment(
                flatId,
                address,
                hasGates,
                initialTab,
                initialPosition,
                clientId
            )
        findNavController().navigate(action)
    }

    private fun initPermanentAddressAccess() {
        permanentAccessAdapter = AsyncListDifferDelegationAdapter<ContactModel>(
            object : DiffUtil.ItemCallback<ContactModel>() {
                override fun areItemsTheSame(oldItem: ContactModel, newItem: ContactModel) =
                    oldItem.number == newItem.number

                override fun areContentsTheSame(oldItem: ContactModel,newItem: ContactModel) =
                    oldItem == newItem
            },
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

        binding.rvPermanentAccessAddress.adapter = permanentAccessAdapter
    }

    private fun showDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)
        if (DataModule.providerConfig.guestAccess == GuestAccessType.TURN_ON_ONLY) {
            builder
                .setTitle(resources.getString(R.string.dialog_title))
                .setMessage(resources.getString(R.string.dialog_message))
                .setCancelable(false)
                .setPositiveButton(resources.getString(R.string.dialog_yes)) { _, _ ->
                    mViewModel.guestAccess(flatId, true)
                    binding.btnGuestAccessOpen.isClickable = false
                }
                .setNegativeButton(resources.getString(R.string.dialog_no)) { _, _ ->
                    binding.btnGuestAccessOpen.isChecked = false
                    returnTransition
                }.show()
        } else {
            binding.btnGuestAccessOpen.isClickable = true
            val isOpen = !binding.btnGuestAccessOpen.isChecked
            builder
                .setTitle(resources.getString(if (isOpen) R.string.dialog_title2 else R.string.dialog_title))
                .setMessage(resources.getString(if (isOpen) R.string.dialog_message2 else R.string.dialog_message))
                .setCancelable(false)
                .setPositiveButton(resources.getString(if (isOpen) R.string.dialog_turn_off else R.string.dialog_yes)) { _, _ ->
                    mViewModel.guestAccess(flatId, !isOpen)
                    binding.btnGuestAccessOpen.isChecked = !isOpen
                }
                .setNegativeButton(resources.getString(R.string.dialog_no)) { _, _ ->
                    binding.btnGuestAccessOpen.isChecked = isOpen
                    returnTransition
                }.show()
        }
    }

    private fun requestCameraPermission() {
        @Suppress("DEPRECATION")
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
                mViewModel.deleteRoommate(flatId, number, clientId)
            }
            .setNegativeButton(resources.getString(R.string.setting_dialog_delete_no)) { _, _ ->
                returnTransition
            }.show()
    }

    private fun onGateAccessByPhoneMenuClick(anchorView: View, contact: ContactModel) {
        showGateAccessDropdownMenu(anchorView, contact = contact)
    }

    private fun onGateAccessByLicensePlateMenuClick(anchorView: View, lpValue: LicensePlateValue) {
        showGateAccessDropdownMenu(anchorView, lpValue = lpValue)
    }

    private fun showGateAccessDropdownMenu(
        anchorView: View,
        lpValue: LicensePlateValue? = null,
        contact: ContactModel? = null
    ) {
        if (lpValue == null && contact == null) return
        val context = anchorView.context
        val popupMenu = PopupMenu(context, anchorView)

        val menu = when {
            lpValue == null -> R.menu.gate_access_by_phone_dropdown_menu
            contact == null -> R.menu.gate_access_by_license_plate_dropdown_menu
            else -> return
        }
        popupMenu.inflate(menu)

        val accentColor = ContextCompat.getColor(context, R.color.accent)
        for (i in 0 until popupMenu.menu.size) {
            val item = popupMenu.menu[i]
            val spannableTitle = SpannableString(item.title)
            spannableTitle.setSpan(
                ForegroundColorSpan(accentColor),
                0,
                spannableTitle.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            item.title = spannableTitle
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_sms -> {
                    contact?.let { mViewModel.resend(flatId, it.number) }
                    true
                }
                R.id.action_delete_phone -> {
                    contact?.let { mViewModel.deleteRoommate(flatId, it.number, clientId) }
                    true
                }
                R.id.action_delete_license_plate -> {
                    lpValue?.let { mViewModel.removeLicensePlate(flatId, it) }
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gateAccessByPhoneManager = null
        gateAccessByLicensePlateManager = null
        _binding = null
        permanentAccessAdapter = null
    }

    companion object {
        const val CARD_ITEMS_FOR_PREVIEW = 5
        const val SHOW_ALL_ITEMS_FOR_PREVIEW = 1
    }
}
