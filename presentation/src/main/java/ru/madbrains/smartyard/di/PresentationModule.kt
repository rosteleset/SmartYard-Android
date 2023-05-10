package ru.madbrains.smartyard.di

import androidx.lifecycle.SavedStateHandle
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.madbrains.smartyard.GlobalDataSource
import ru.madbrains.smartyard.ui.call.IncomingCallActivityViewModel
import ru.madbrains.smartyard.ui.common.AppealFormViewModel
import ru.madbrains.smartyard.ui.launcher.LauncherViewModel
import ru.madbrains.smartyard.ui.main.ChatWoot.ChatWootViewModel
import ru.madbrains.smartyard.ui.main.MainActivityViewModel
import ru.madbrains.smartyard.ui.main.address.AddressViewModel
import ru.madbrains.smartyard.ui.main.address.addressVerification.courier.CourierViewModel
import ru.madbrains.smartyard.ui.main.address.addressVerification.office.OfficeViewModel
import ru.madbrains.smartyard.ui.main.address.auth.AuthViewModel
import ru.madbrains.smartyard.ui.main.address.auth.restoreAccess.RestoreAccessViewModel
import ru.madbrains.smartyard.ui.main.address.auth.restoreAccess.codeSmsRestore.CodeSmsRestoreViewModel
import ru.madbrains.smartyard.ui.main.address.availableServices.AvailableServicesViewModel
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVTrimmerViewModel
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVViewModel
import ru.madbrains.smartyard.ui.main.address.event_log.EventLogViewModel
import ru.madbrains.smartyard.ui.main.address.inputAdress.InputAddressViewModel
import ru.madbrains.smartyard.ui.main.address.noNetwork.NoNetworkViewModel
import ru.madbrains.smartyard.ui.main.address.qrCode.QrCodeViewModel
import ru.madbrains.smartyard.ui.main.address.workSoon.courier.WorkSoonCourierViewModel
import ru.madbrains.smartyard.ui.main.address.workSoon.office.WorkSoonOfficeViewModel
import ru.madbrains.smartyard.ui.main.burger.BurgerViewModel
import ru.madbrains.smartyard.ui.main.burger.ExtWebViewModel
import ru.madbrains.smartyard.ui.main.burger.cityCameras.CityCamerasViewModel
import ru.madbrains.smartyard.ui.main.chat.ChatViewModel
import ru.madbrains.smartyard.ui.main.notification.NotificationViewModel
import ru.madbrains.smartyard.ui.main.pay.PayAddressViewModel
import ru.madbrains.smartyard.ui.main.pay.contract.dialogPay.PayBottomSheetDialogViewModel
import ru.madbrains.smartyard.ui.main.pay.contract.webview.PayWebViewViewModel
import ru.madbrains.smartyard.ui.main.settings.SettingsViewModel
import ru.madbrains.smartyard.ui.main.settings.accessAddress.AccessAddressViewModel
import ru.madbrains.smartyard.ui.main.settings.accessAddress.dialogDeleteReason.DialogDeleteReasonViewModel
import ru.madbrains.smartyard.ui.main.settings.addressSettings.AddressSettingsViewModel
import ru.madbrains.smartyard.ui.main.settings.basicSettings.BasicSettingsViewModel
import ru.madbrains.smartyard.ui.main.settings.faceSettings.FaceSettingsViewModel
import ru.madbrains.smartyard.ui.onboarding.OnboardingViewModel
import ru.madbrains.smartyard.ui.reg.RegistrationViewModel
import ru.madbrains.smartyard.ui.reg.sms.SmsRegViewModel
import ru.madbrains.smartyard.ui.reg.tel.NumberRegViewModel

object PresentationModule {
    fun create() = module {
        viewModel { LauncherViewModel(get()) }
        viewModel { OnboardingViewModel(get()) }
        viewModel { RegistrationViewModel(get(), get()) }
        viewModel { SmsRegViewModel(get(), get()) }
        viewModel { NumberRegViewModel(get(), get()) }
        viewModel { AppealFormViewModel(get(), get()) }
        viewModel { IncomingCallActivityViewModel(get()) }
        viewModel { InputAddressViewModel(get()) }
        viewModel { AddressSettingsViewModel(get(), get(), get(), get()) }
        viewModel { AccessAddressViewModel(get(), get()) }
        viewModel { BasicSettingsViewModel(get(), get(), get()) }
        viewModel { AuthViewModel(get(), get(), get(), get()) }
        viewModel { OfficeViewModel(get(), get(), get(), get()) }
        viewModel { RestoreAccessViewModel(get()) }
        viewModel { CodeSmsRestoreViewModel(get()) }
        viewModel { QrCodeViewModel(get()) }
        viewModel { NotificationViewModel(get()) }
        viewModel { NoNetworkViewModel(get(), get(), get()) }
        viewModel { CourierViewModel(get(), get(), get()) }
        viewModel { WorkSoonOfficeViewModel(get(), get(), get()) }
        viewModel { WorkSoonCourierViewModel(get(), get(), get()) }
        viewModel { DialogDeleteReasonViewModel(get(), get(), get(), get()) }
        viewModel { AvailableServicesViewModel(get(), get(), get()) }
        viewModel { MainActivityViewModel(get(), get(), get(), get()) }
        viewModel { ChatViewModel(get()) }
        viewModel { SettingsViewModel(get(), get(), get()) }
        viewModel { AddressViewModel(get(), get(), get(), get(), get()) }
        viewModel { EventLogViewModel(get(), get()) }
        viewModel { FaceSettingsViewModel(get(), get()) }
        viewModel { (handle: SavedStateHandle) -> CCTVViewModel(handle, get(), get()) }
        viewModel { (handle: SavedStateHandle) -> CityCamerasViewModel(handle, get(), get(), get()) }
        viewModel { BurgerViewModel( get(), get(), get(), get()) }
        viewModel { CCTVTrimmerViewModel(get()) }
        viewModel { PayAddressViewModel(get()) }
        viewModel { PayBottomSheetDialogViewModel(get()) }
        viewModel { PayWebViewViewModel(get()) }
        viewModel { ExtWebViewModel() }
        viewModel { ChatWootViewModel(get()) }
        single { GlobalDataSource() }
    }
}
