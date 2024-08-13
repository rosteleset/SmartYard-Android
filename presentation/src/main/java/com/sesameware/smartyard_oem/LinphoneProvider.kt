package com.sesameware.smartyard_oem

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.model.PushCallData
import com.sesameware.domain.utils.doDelayed
import com.sesameware.smartyard_oem.ui.SoundChooser
import com.sesameware.smartyard_oem.ui.call.AndroidAudioManager
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity.Companion.PUSH_DATA
import com.sesameware.smartyard_oem.ui.sendCallNotification
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.linphone.core.AccountCreator
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.ProxyConfig
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import timber.log.Timber

class LinphoneProvider(val core: Core, val service: LinphoneService) : KoinComponent {
    private var currentRingtone: Ringtone? = null
    var pushCallData: PushCallData? = null
    private val preferenceStorage: PreferenceStorage by inject()

    val registrationState = MutableLiveData(
        CRegistrationState(RegistrationState.None)
    )
    val callState = MutableLiveData(CCallState(Call.State.Idle))
    val dtmfIsSent = MutableLiveData(false)
    val finishCallActivity = MutableLiveData<Event<Unit>>()
    var mAudioManager: AndroidAudioManager = AndroidAudioManager(service)

    private var shouldVibrate = false
    private var vibrationPattern = longArrayOf(0, 1000, 1000)

    private var speakerDevice: AudioDevice? = null
    private var earpieceDevice: AudioDevice? = null

    @Suppress("DEPRECATION")
    private var mCoreListener = object : CoreListenerStub() {
        override fun onRegistrationStateChanged(
            core: Core,
            proxyConfig: ProxyConfig,
            state: RegistrationState,
            message: String
        ) {
            Timber.d("debug_dmm reg_state: $state message: $message")
            registrationState.value = CRegistrationState(state, message)
            when (state) {
                RegistrationState.Failed -> {
                    service.stopSelf()
                }
                else -> {
                }
            }
            super.onRegistrationStateChanged(core, proxyConfig, state, message)
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            Timber.d("debug_dmm call_state: $state message: $message")
            val cState = CCallState(state, message, call, core)
            callState.value = cState

            when (cState.state) {
                CallStateSimple.INCOMING -> {
                    notifyIncomingCall()
                }
                CallStateSimple.END,
                CallStateSimple.ERROR -> {
                    service.stopSelf()
                }
                CallStateSimple.CONNECTED -> {
                    stopRinging()
                }
                CallStateSimple.OTHER_CONNECTED,
                CallStateSimple.IDLE,
                CallStateSimple.CONNECTING -> {
                }
            }
            super.onCallStateChanged(core, call, state, message)
        }
    }

    private fun notifyIncomingCall() {
        pushCallData?.let { data ->
            val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notification = notificationManager.getNotificationChannel(MessagingService.CHANNEL_CALLS_ID)
                shouldVibrate = notification?.shouldVibrate() ?: false
                vibrationPattern = notification?.vibrationPattern ?: vibrationPattern
            }
            if (data.flagNotification) {
                sendCallNotification(data, service, preferenceStorage)
            } else {
                val intent =
                    Intent(service, IncomingCallActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                        putExtra(PUSH_DATA, data)
                    }
                service.startActivity(intent)
            }
            startRinging()
        }
    }

    fun setNativeVideoWindowId(videoWindow: View) {
        core.nativeVideoWindowId = videoWindow
    }

    private fun deleteCallNotifications(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(preferenceStorage.notificationData.currentCallId)
    }

    fun isConnected(): Boolean {
        return callState.value?.state == CallStateSimple.CONNECTED ||
            callState.value?.state == CallStateSimple.OTHER_CONNECTED
    }


    fun isVideoCall(): Boolean = if (isConnected()) {
        core.currentCall?.remoteParams?.isVideoEnabled ?: false
    } else {
        false
    }

    fun listenAndGetNotifications(pendingData: PushCallData) {
        val ring = SoundChooser.getChosenTone(
            service,
            RingtoneManager.TYPE_RINGTONE,
            pendingData.flatId,
            preferenceStorage
        )

        Timber.d("debug_dmm ring.uri: ${ring.uri}")

        currentRingtone = RingtoneManager.getRingtone(service, ring.uri)
        pushCallData = pendingData
        connect(pendingData)
    }

    private fun startRinging() {
        currentRingtone?.play()
        if (shouldVibrate) {
            mAudioManager.vibrator?.vibrate(vibrationPattern, 0)
        }
    }

    fun stopRinging() {
        currentRingtone?.stop()
        deleteCallNotifications(service)
        mAudioManager.vibrator?.cancel()
    }

    fun acceptCall() {
        core.currentCall?.let { call ->
            val params = core.createCallParams(call)
            params?.isVideoEnabled = true
            params?.isAudioEnabled = true
            call.acceptWithParams(params)
        }
    }

    fun acceptCallForDoor() {
        core.currentCall?.let { call ->
            val params = core.createCallParams(call)
            params?.isVideoEnabled = true
            params?.isAudioEnabled = true
            call.acceptWithParams(params)
        }
    }

    fun resume() {
        if (isConnected()) {
            core.addListener(mCoreListener)
        }
    }

    fun pause() {
        core.removeListener(mCoreListener)
    }

    fun onDestroy() {
        stopRinging()
        disconnect()
        finishCallActivity.value = Event(Unit)
    }

    private fun connect(data: PushCallData) {
        val config = SipConfig(
            server = data.server,
            username = data.extension,
            password = data.pass,
            port = data.port.toInt(),
            protocol = data.transport.convert()
        )
        if (config.validate()) {
            core.let { core ->
                core.removeListener(mCoreListener)
                val mAccountCreator = core.createAccountCreator(null)
                @Suppress("DEPRECATION") val cfg = config.setAccount(mAccountCreator).createProxyConfig()
                core.addProxyConfig(cfg!!)
                core.ringback = null
                core.ring = null

                core.isVideoDisplayEnabled = true
                core.isVideoCaptureEnabled = true
                core.useInfoForDtmf = false
                core.useRfc2833ForDtmf = true
                for (pt in core.audioPayloadTypes) {
                    pt.enable(true)
                }
                for (d in core.videoDevicesList) {
                    if (d == NO_VIDEO_MODE) {
                        core.videoDevice = d
                    }
                }
                for (pt in core.videoPayloadTypes) {
                    if (pt.mimeType == H264) {
                        pt.enable(true)
                    } else {
                        pt.enable(false)
                    }
                }
                core.addListener(mCoreListener)
            }
        }
    }

    fun disconnect() {
        core.clearProxyConfig()
        core.terminateAllCalls()
        core.removeListener(mCoreListener)
        registrationState.value = CRegistrationState(RegistrationState.None)
    }

    fun reset() {
        dtmfIsSent.value = false
    }

    fun sendDtmf() {
        Timber.d("debug_dmm sending dtmf...")
        core.currentCall?.run {
            doDelayed(
                {
                    val dtmfs = "${pushCallData?.dtmf}${pushCallData?.dtmf}${pushCallData?.dtmf}"
                    sendDtmfs(dtmfs)
                    doDelayed(
                        {
                            Timber.d("debug_dmm dtmf sent")
                            dtmfIsSent.value = true
                        },
                        1000
                    )
                },
                1000
            )
        }
    }

    fun getCallDuration(): Int {
        return core.currentCall?.duration ?: 0
    }

    private fun routeAudioTo(type: AudioDevice.Type) {
        val capability = AudioDevice.Capabilities.CapabilityPlay
        val extendedAudioDevices = core.extendedAudioDevices
        if (speakerDevice == null || earpieceDevice == null) {
            extendedAudioDevices.forEach {
                if (it.hasCapability(capability)) {
                    Timber.d("__L__  name = ${it.deviceName}    driver = ${it.driverName}    ${it.type}")
                    if (it.driverName.contains("openSLES", true)) {
                        if (it.type == AudioDevice.Type.Speaker) {
                            speakerDevice = it
                        }
                        if (it.type == AudioDevice.Type.Earpiece) {
                            earpieceDevice = it
                        }
                    }
                }
            }
        }
        if (type == AudioDevice.Type.Speaker) {
            core.currentCall?.outputAudioDevice = speakerDevice
        } else {
            core.currentCall?.outputAudioDevice = earpieceDevice
        }
    }

    fun routeAudioToSpeaker() {
        routeAudioTo(AudioDevice.Type.Speaker)
    }

    fun routeAudioToEarpiece() {
        routeAudioTo(AudioDevice.Type.Earpiece)
    }

    companion object {
        const val NO_VIDEO_MODE = "StaticImage: Static picture"
        const val H264 = "H264"
    }
}

class SipConfig(
    var server: String = DataModule.BASE_URL,
    var port: Int = 54674,
    var username: String = "1002",
    var password: String = "ieNg8oof",
    var protocol: TransportType = TransportType.Tls
) {

    fun validate(): Boolean {
        return server.isNotEmpty() && port != 0 && username.isNotEmpty() && password.isNotEmpty()
    }

    fun getAddress(): String {
        return "$server:$port"
    }

    fun setAccount(creator: AccountCreator): AccountCreator {
        creator.domain = getAddress()
        creator.transport = protocol
        creator.username = username
        creator.password = password
        return creator
    }
}

data class CRegistrationState(
    val state: RegistrationState,
    val message: String = ""
)

class CCallState(
    callState: Call.State,
    val message: String = "",
    val call: Call? = null,
    val core: Core? = null
) {
    val state: CallStateSimple = convertCallState(callState)
}

private fun convertCallState(state: Call.State): CallStateSimple {
    return when (state) {
        Call.State.IncomingReceived -> {
            CallStateSimple.INCOMING
        }
        Call.State.Connected -> {
            CallStateSimple.CONNECTED
        }
        Call.State.OutgoingInit,
        Call.State.OutgoingProgress -> {
            CallStateSimple.CONNECTING
        }

        Call.State.Error -> {
            CallStateSimple.ERROR
        }
        Call.State.Idle -> {
            CallStateSimple.IDLE
        }
        Call.State.End,
        Call.State.Released -> {
            CallStateSimple.END
        }
        Call.State.StreamsRunning,
        Call.State.Updating,
        Call.State.PausedByRemote,
        Call.State.UpdatedByRemote,
        Call.State.Pausing,
        Call.State.Paused,
        Call.State.Resuming,
        Call.State.Referred,
        Call.State.EarlyUpdatedByRemote,
        Call.State.EarlyUpdating,
        Call.State.IncomingEarlyMedia,
        Call.State.OutgoingRinging,
        Call.State.PushIncomingReceived,
        Call.State.OutgoingEarlyMedia -> {
            CallStateSimple.OTHER_CONNECTED
        }
    }
}

enum class CallStateSimple {
    CONNECTED, CONNECTING, ERROR, IDLE, INCOMING, END, OTHER_CONNECTED
}
