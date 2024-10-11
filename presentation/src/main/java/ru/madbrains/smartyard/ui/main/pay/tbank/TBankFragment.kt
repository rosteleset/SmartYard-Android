package ru.madbrains.smartyard.ui.main.pay.tbank

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.FragmentTBankBinding
import ru.madbrains.smartyard.ui.main.address.AddressViewModel
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.dateTimeFormat
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.mainform.MainFormLauncher
import ru.tinkoff.acquiring.sdk.redesign.sbp.SbpPayLauncher
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.utils.Money
import timber.log.Timber
import java.util.Date
import java.util.UUID


class TBankFragment : Fragment() {
    lateinit var binding: FragmentTBankBinding
    private val launcherSbp =
        registerForActivityResult(SbpPayLauncher.Contract, ::handlePaymentResult)
    private val launcherNewCard =
        registerForActivityResult(MainFormLauncher.Contract, ::handleNewCardPaymentResult)
    private val mViewModel by sharedViewModel<AddressViewModel>()

    private var summa = 0.0
    private var title = ""
    private var merchant = ""
    private var tKey = ""
    private var pKey = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            summa = getDouble("summa")
            title = getString("title") ?: ""
            merchant = getString("merchant") ?: ""
        }
        when (merchant) {
            "centra" -> {
                tKey = getString(R.string.TERMINAL_TBANK_CENTRA_KEY)
                pKey = getString(R.string.PUBLIC_TBANK_CENTRA_KEY)
            }

            "layka" -> {
                tKey = getString(R.string.TERMINAL_TBANK_CENTRA_KEY)
                pKey = getString(R.string.PUBLIC_TBANK_CENTRA_KEY)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTBankBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AcquiringSdk.isDebug = true
        AcquiringSdk.isDeveloperMode = false
        startPay()
    }

    private fun startPay() {
        val tinkoffAcquiring = TinkoffAcquiring(
            context = requireContext(),
            terminalKey = tKey,
            publicKey = pKey
        )
        tinkoffAcquiring.checkTerminalInfo(
            onSuccess = { terminalInfo ->
                if (terminalInfo?.paymethods?.any { it.paymethod == Paymethod.SBP } == true) {
                    startPayment(tinkoffAcquiring)
                }
            },
            onFailure = { e ->
                Timber.e(e)
            }
        )
    }

    private fun destroyFragment() {
        val fragmentManager = requireActivity().supportFragmentManager
        fragmentManager.popBackStack()
    }

    private fun generateUUID() =
        UUID.nameUUIDFromBytes("${System.currentTimeMillis()}$title".toByteArray()).toString()

    private fun startCardsPayment() {
        val paymentOptions = PaymentOptions().setOptions {
            setTerminalParams(
                terminalKey = tKey,
                publicKey = pKey
            )
            orderOptions {
                orderId = generateUUID()
                amount = Money.ofRubles(summa)
                recurrentPayment = false
            }
        }
        val startData = MainFormLauncher.StartData(paymentOptions)
        launcherNewCard.launch(startData)
    }

    private fun handleNewCardPaymentResult(result: MainFormLauncher.Result) {
        when (result) {
            MainFormLauncher.Canceled -> {
                Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_LONG).show()
                destroyFragment()
            }

            is MainFormLauncher.Error -> Toast.makeText(
                requireContext(),
                "Error",
                Toast.LENGTH_LONG
            ).show()

            is MainFormLauncher.Success -> {
                Toast.makeText(requireContext(), "Success", Toast.LENGTH_LONG).show()
            }

            else -> destroyFragment()
        }
    }

    private fun startPayment(tinkoffAcquiring: TinkoffAcquiring) {
        tinkoffAcquiring.initSbpPaymentSession()
        val options = PaymentOptions().setOptions {
            setTerminalParams(
                terminalKey = tKey,
                publicKey = pKey
            )
            orderOptions {
                orderId = generateUUID()
                amount = Money.ofRubles(summa)
                recurrentPayment = false
            }
        }
        val startData = SbpPayLauncher.StartData(options)
        launcherSbp.launch(startData)
    }

    private fun handlePaymentResult(result: SbpPayLauncher.Result) {
        when (result) {
            is SbpPayLauncher.Canceled -> {
                Toast.makeText(requireContext(), "Отменено", Toast.LENGTH_LONG).show()
                destroyFragment()
            }

            is SbpPayLauncher.Error -> {
                destroyFragment()
                Toast.makeText(
                    requireContext(),
                    "Ошибка: ${result.error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

            is SbpPayLauncher.Success -> {
                //TODO Вынести от сюда
                mViewModel.newSbpPay(title, merchant, summa, "T-Bank SBP Pay TEST")
                mViewModel.sbpPayOrder.observe(viewLifecycleOwner) { item ->
                    item?.let {
                        mViewModel.checkSbpPay(
                            merchant = merchant,
                            id = item.id.toString(),
                            status = 2,
                            orderId =  result.payment.toString(),
                            processed = item.createdAt,
                            test = "t"
                        )
                        destroyFragment()
                    }
                }
                Toast.makeText(
                    requireContext(),
                    "Платеж выполнен успешно. paymentId = ${result.payment}",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> destroyFragment()
        }
    }
}