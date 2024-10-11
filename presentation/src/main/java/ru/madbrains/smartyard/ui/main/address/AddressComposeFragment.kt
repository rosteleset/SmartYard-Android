package ru.madbrains.smartyard.ui.main.address

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.Calendar
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentAddressComposeBinding
import ru.madbrains.smartyard.ui.getStatusBarHeight
import ru.madbrains.smartyard.ui.main.address.auth.AuthFragment
import ru.madbrains.smartyard.ui.main.intercom.BroadCastManager
import ru.madbrains.smartyard.ui.main.notification.NotificationFragment
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.dateFormatRu
import ru.madbrains.smartyard.ui.theme.ComposeAppTheme
import timber.log.Timber


data class PayButton(
    val image: Int,
    val title: String,
    val subTitle: String? = null,
    val type: PayButtonType,
    val bindingId: String? = null,
    val isAutoPay: Boolean? = null
)

data class DataForPay(
    val summa: Double,
    val method: PayButtonType,
    val email: String,
    val isAutoPay: Boolean,
    val isSaveCard: Boolean,
    val sendBill: String,
    val bindingId: String,
    val merchant: String,
    val contractTitle: String
)

enum class PayButtonType(val paymentSystem: String) {
    NEW_CARD("NEW_CARD"),
    SBP("Sbp"),
    SECURE("SECURE"),
    VISA("Visa"),
    MIR("Mir"),
    CREDIT_PAY("CREDIT_PAY"),
    MASTER_CARD("MasterCard"),

}

class RangeValidator(private val minDate: Long, private val maxDate: Long) :
    CalendarConstraints.DateValidator {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong()
    )

    override fun describeContents(): Int {
        TODO("Not yet implemented")
    }

    override fun writeToParcel(p0: Parcel, p1: Int) {
    }

    override fun isValid(date: Long): Boolean {
        return !(minDate > date || maxDate < date)

    }

    companion object CREATOR : Parcelable.Creator<RangeValidator> {
        override fun createFromParcel(parcel: Parcel): RangeValidator {
            return RangeValidator(parcel)
        }

        override fun newArray(size: Int): Array<RangeValidator?> {
            return arrayOfNulls(size)
        }
    }

}

class AddressComposeFragment : Fragment() {
    lateinit var binding: FragmentAddressComposeBinding
    private val mAddressViewModel by sharedViewModel<AddressViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAddressViewModel.getContracts()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddressComposeBinding.inflate(inflater, container, false).apply {
            cvAddress.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    ComposeAppTheme {
                        MainAddress(
                            mViewModel = mAddressViewModel,
                            startCalendar = { id ->
                                setupRangePickerDialog(id)
                            },
                            onNavigate = { fragment, bundle ->
                                navigateToFragment(
                                    id = R.id.fl_address_compose_fragment,
                                    fragment = fragment,
                                    bundle = bundle
                                )
                            }
                        )
                    }
                }
            }
        }
        return binding.root
    }

    private val receiver = object : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
            when(p1?.action){
                REFRESH_INTENT -> {
                    mAddressViewModel.getContracts()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(REFRESH_INTENT)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val statusBarHeight = getStatusBarHeight(requireContext())
        observers()
        binding.clCityMain.setPadding(0, statusBarHeight + 1, 0, 0)
        binding.ivAddContract.setOnClickListener {
            navigateToFragment(R.id.fl_address_compose_fragment, AuthFragment())
        }
        binding.ivNoti.setOnClickListener {
            navigateToFragment(R.id.fl_address_compose_fragment, NotificationFragment())
        }
    }

    private fun observers() {
        mAddressViewModel.city.observe(viewLifecycleOwner) {
            binding.tvTitleCity.text = it
        }
        mAddressViewModel.payState.observe(viewLifecycleOwner) {
            if (it != null) {
                val intent = when (it.status) {
                    0 -> Intent(PAY_STATUS_IN_PROCESS)
                    2 -> Intent(PAY_STATUS_SUCCESS)
                    3 -> Intent(PAY_STATUS_CANCELED).putExtra("comment", it.comment)
                    6 -> Intent(PAY_STATUS_ERROR)
                    else -> Intent(PAY_STATUS_ERROR)
                }
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
            }
        }
    }


    private fun navigateToFragment(id: Int, fragment: Fragment, bundle: Bundle? = null) {
        val transaction = parentFragmentManager.beginTransaction()
        val existingFragment =
            parentFragmentManager.findFragmentByTag(fragment.javaClass.simpleName)
        if (existingFragment != null) {
            transaction.show(existingFragment)
        } else {
            if (bundle != null) {
                fragment.arguments = bundle
            }
            transaction.add(id, fragment, fragment.javaClass.simpleName)
            if (!shouldReturnToMainScreen(fragment)) {
                transaction.addToBackStack("AddressComposeFragment")
            }
        }
        val currentFragment = parentFragmentManager.findFragmentById(id)
        if (currentFragment != null) {
            transaction.hide(currentFragment)
        }
        transaction.commit()
    }

    private fun shouldReturnToMainScreen(fragment: Fragment): Boolean =
        fragment is AddressComposeFragment


    private fun setupRangePickerDialog(id: String) {
        val builderRange = MaterialDatePicker.Builder.dateRangePicker()
        builderRange.setCalendarConstraints(limitRange().build())
        val pickerRange = builderRange.build()
        pickerRange.show(childFragmentManager, pickerRange.toString())
        pickerRange.addOnPositiveButtonClickListener {
            mAddressViewModel.getBalanceDetail(
                id,
                dateFormatRu.format(it.second),
                dateFormatRu.format(it.first)
            )
        }
    }

    private fun limitRange(): CalendarConstraints.Builder {
        val constraintsBuilderRange = CalendarConstraints.Builder()

        val calendarStart: Calendar = Calendar.getInstance()
        val calendarEnd: Calendar = Calendar.getInstance()

        calendarStart.set(calendarEnd.get(Calendar.YEAR) - 1, 0, 0)
        calendarEnd.set(
            calendarEnd.get(Calendar.YEAR),
            calendarEnd.get(Calendar.MONTH),
            calendarEnd.get(Calendar.DAY_OF_MONTH)
        )

        val minDate = calendarStart.timeInMillis
        val maxDate = calendarEnd.timeInMillis

        constraintsBuilderRange.setStart(minDate)
        constraintsBuilderRange.setEnd(maxDate)

        constraintsBuilderRange.setValidator(RangeValidator(minDate, maxDate))

        return constraintsBuilderRange
    }

    companion object {
        const val REFRESH_INTENT = "REFRESH_INTENT"
        const val PAY_STATUS_SUCCESS = "PAY_STATUS_SUCCESS"
        const val PAY_STATUS_ERROR = "PAY_STATUS_ERROR"
        const val PAY_STATUS_CANCELED = "PAY_STATUS_CANCELED"
        const val PAY_STATUS_IN_PROCESS = "PAY_STATUS_IN_PROCESS"
        const val BROADCAST_CONFIRME_STATUS_PAY = "BROADCAST_CONFIRME_STATUS_PAY"
    }
}


