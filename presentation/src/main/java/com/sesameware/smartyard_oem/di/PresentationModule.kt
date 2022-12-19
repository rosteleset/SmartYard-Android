package com.sesameware.smartyard_oem.di

import androidx.lifecycle.SavedStateHandle
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.sesameware.smartyard_oem.GlobalDataSource
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivityViewModel
import com.sesameware.smartyard_oem.ui.common.AppealFormViewModel
import com.sesameware.smartyard_oem.ui.launcher.LauncherViewModel
import com.sesameware.smartyard_oem.ui.main.MainActivityViewModel
import com.sesameware.smartyard_oem.ui.main.address.AddressViewModel
import com.sesameware.smartyard_oem.ui.main.address.addressVerification.courier.CourierViewModel
import com.sesameware.smartyard_oem.ui.main.address.addressVerification.office.OfficeViewModel
import com.sesameware.smartyard_oem.ui.main.address.auth.AuthViewModel
import com.sesameware.smartyard_oem.ui.main.address.auth.restoreAccess.RestoreAccessViewModel
import com.sesameware.smartyard_oem.ui.main.address.auth.restoreAccess.codeSmsRestore.CodeSmsRestoreViewModel
import com.sesameware.smartyard_oem.ui.main.address.availableServices.AvailableServicesViewModel
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.CCTVTrimmerViewModel
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.CCTVViewModel
import com.sesameware.smartyard_oem.ui.main.address.event_log.EventLogViewModel
import com.sesameware.smartyard_oem.ui.main.address.inputAdress.InputAddressViewModel
import com.sesameware.smartyard_oem.ui.main.address.noNetwork.NoNetworkViewModel
import com.sesameware.smartyard_oem.ui.main.address.qrCode.QrCodeViewModel
import com.sesameware.smartyard_oem.ui.main.address.workSoon.courier.WorkSoonCourierViewModel
import com.sesameware.smartyard_oem.ui.main.address.workSoon.office.WorkSoonOfficeViewModel
import com.sesameware.smartyard_oem.ui.main.burger.BurgerViewModel
import com.sesameware.smartyard_oem.ui.main.burger.cityCameras.CityCamerasViewModel
import com.sesameware.smartyard_oem.ui.main.chat.ChatViewModel
import com.sesameware.smartyard_oem.ui.main.notification.NotificationViewModel
import com.sesameware.smartyard_oem.ui.main.pay.PayAddressViewModel
import com.sesameware.smartyard_oem.ui.main.pay.contract.dialogPay.PayBottomSheetDialogViewModel
import com.sesameware.smartyard_oem.ui.main.pay.contract.webview.PayWebViewViewModel
import com.sesameware.smartyard_oem.ui.main.settings.SettingsViewModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.AccessAddressViewModel
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.dialogDeleteReason.DialogDeleteReasonViewModel
import com.sesameware.smartyard_oem.ui.main.settings.addressSettings.AddressSettingsViewModel
import com.sesameware.smartyard_oem.ui.main.settings.basicSettings.BasicSettingsViewModel
import com.sesameware.smartyard_oem.ui.main.settings.faceSettings.FaceSettingsViewModel
import com.sesameware.smartyard_oem.ui.onboarding.OnboardingViewModel
import com.sesameware.smartyard_oem.ui.reg.RegistrationViewModel
import com.sesameware.smartyard_oem.ui.reg.outgoing_call.OutgoingCallViewModel
import com.sesameware.smartyard_oem.ui.reg.sms.SmsRegViewModel
import com.sesameware.smartyard_oem.ui.reg.tel.NumberRegViewModel
import com.sesameware.smartyard_oem.ui.reg.providers.ProvidersViewModel

object PresentationModule {
    fun create() = module {
        viewModel { LauncherViewModel(get()) }
        viewModel { ProvidersViewModel(get(), get()) }
        viewModel { OutgoingCallViewModel(get(), get()) }
        viewModel { OnboardingViewModel(get()) }
        viewModel { RegistrationViewModel(get(), get(), get()) }
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
        single { GlobalDataSource() }
    }
}
