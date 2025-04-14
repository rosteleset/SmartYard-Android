package com.sesameware.smartyard_oem

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.sesameware.data.DataModule
import com.sesameware.domain.model.PushCallData
import com.sesameware.domain.utils.doDelayed
import org.koin.core.component.KoinComponent
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
    var pushCallData: PushCallData? = null

    val registrationState = MutableLiveData(
        CRegistrationState(RegistrationState.None)
    )
    val callState = MutableLiveData(CCallState(Call.State.Idle))
    val dtmfIsSent = MutableLiveData(false)
    val finishCallActivity = MutableLiveData<Event<Unit>>()

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
                    Timber.d("debug_dmm    call is ok")
                    service.isCallOk = true
                }
                CallStateSimple.END,
                CallStateSimple.ERROR -> {
                    service.stopSelf()
                }
                CallStateSimple.CONNECTED -> {
                }
                CallStateSimple.OTHER_CONNECTED,
                CallStateSimple.IDLE,
                CallStateSimple.CONNECTING -> {
                }
                CallStateSimple.STREAMS_RUNNING -> {
                }
                CallStateSimple.PAUSED -> {
                }
            }
            super.onCallStateChanged(core, call, state, message)
        }
    }

    fun setNativeVideoWindowId(videoWindow: View) {
        core.nativeVideoWindowId = videoWindow
    }

    fun isConnected(): Boolean {
        return callState.value?.state == CallStateSimple.CONNECTED
                || callState.value?.state == CallStateSimple.OTHER_CONNECTED
                || callState.value?.state == CallStateSimple.PAUSED
                || callState.value?.state == CallStateSimple.STREAMS_RUNNING
    }

    fun isVideoCall(): Boolean = if (isConnected()) {
        core.currentCall?.remoteParams?.isVideoEnabled == true
    } else {
        false
    }

    fun startConnection(data: PushCallData) {
        service.connectionStarted = true
        pushCallData = data
        connect(data)
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

    fun onDestroy() {
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
        Call.State.StreamsRunning -> {
            CallStateSimple.STREAMS_RUNNING
        }
        Call.State.Pausing,
        Call.State.Paused -> {
            CallStateSimple.PAUSED
        }
        Call.State.Updating,
        Call.State.PausedByRemote,
        Call.State.UpdatedByRemote,
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
    CONNECTED, CONNECTING, ERROR, IDLE, INCOMING, END, OTHER_CONNECTED, STREAMS_RUNNING, PAUSED
}
