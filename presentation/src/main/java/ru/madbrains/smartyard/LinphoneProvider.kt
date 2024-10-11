package ru.madbrains.smartyard

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.sip.SipAudioCall
import android.net.sip.SipSession
import android.os.Build
import android.os.PowerManager
import android.view.TextureView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.linphone.core.Account
import org.linphone.core.AccountCreator
import org.linphone.core.Call
import org.linphone.core.CallStats
import org.linphone.core.ConfiguringState
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.GlobalState
import org.linphone.core.ProxyConfig
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import ru.madbrains.data.DataModule
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.model.FcmCallData
import ru.madbrains.domain.utils.doDelayed
import ru.madbrains.smartyard.ui.SoundChooser
import ru.madbrains.smartyard.ui.call.AndroidAudioManager
import ru.madbrains.smartyard.ui.call.IncomingCallActivity
import ru.madbrains.smartyard.ui.call.IncomingCallActivity.Companion.FCM_DATA
import ru.madbrains.smartyard.ui.sendCallNotification
import timber.log.Timber

class LinphoneProvider(val core: Core, val service: LinphoneService) : KoinComponent {
    private var currentRingtone: Ringtone? = null
    private var fcmData: FcmCallData? = null
    private val preferenceStorage: PreferenceStorage by inject()

    val registrationState = MutableLiveData(
        CRegistrationState(RegistrationState.None)
    )
    val callState = MutableLiveData(CCallState(Call.State.Idle))
    val dtmfIsSent = MutableLiveData(false)
    val finishCallActivity = MutableLiveData<Event<Unit>>()
    var mAudioManager: AndroidAudioManager = AndroidAudioManager(service)

    private var mCoreListener = object : CoreListenerStub() {
//        override fun onRegistrationStateChanged(
//            core: Core,
//            proxyConfig: ProxyConfig,
//            state: RegistrationState?,
//            message: String
//        ) {
//            Timber.d("debug_dmm reg_state: $state message: $message")
//            Timber.d("debsssug_dmm reg_state: $state message: $message")
//            state?.let {
//                registrationState.value = CRegistrationState(state, message)
//            }
//            when (state) {
//                RegistrationState.Failed -> {
//                    Timber.d("debsssug_dmm reg_state ERROR state:${state}")
//                    service.stopSelf()
//                }
//
//                else -> {
//                }
//            }
//            super.onRegistrationStateChanged(core, proxyConfig, state, message)
//        }

        override fun onGlobalStateChanged(core: Core, state: GlobalState?, message: String) {
            super.onGlobalStateChanged(core, state, message)
            Timber.d("debug_dmm ONGLOADLBLSTATBG state:$state message:$message")
        }


        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            Timber.d("debug_dmm reg_state: $state message: $message")

            state?.let {
                registrationState.value = CRegistrationState(state, message)
            }
            when (state) {
                RegistrationState.Failed -> {
                    service.stopSelf()
                }
                else -> {}
            }
            super.onAccountRegistrationStateChanged(core, account, state, message)
        }

        private fun isAppVisible(): Boolean {
            return ProcessLifecycleOwner
                .get()
                .lifecycle
                .currentState
                .isAtLeast(Lifecycle.State.STARTED)
        }

        private fun isAppInScreenLock(): Boolean =
            (service.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked


        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            Timber.d("debug_dmm call_state: $state message: $message")
            val cState = state?.let { CCallState(it, message, call, core) }
            callState.value = cState
            if (cState != null) {
                when (cState.state) {
                    CallStateSimple.INCOMING -> {
                        wakeLock()
                        try {
                            call.let { _call ->
                                fcmData?.let { data ->
                                    if (!isAppVisible()) {
                                        if (isAppInScreenLock()) {
                                            val nm = NotificationManagerCompat.from(service)
                                            val id = (100..999).random()
                                            val channelId = "LockScreenNotificationChannel"

                                            val notificationBuilder =
                                                NotificationCompat.Builder(service, channelId)

                                            val fullScreenIntent =
                                                Intent(service, IncomingCallActivity::class.java)
                                            fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            fullScreenIntent.putExtra(FCM_DATA, data)

                                            val fullScreenPendingIntent =
                                                PendingIntent.getActivity(
                                                    service,
                                                    0,
                                                    fullScreenIntent,
                                                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                                    } else {
                                                        PendingIntent.FLAG_UPDATE_CURRENT
                                                    }
                                                )

                                            val notification = notificationBuilder
                                                .setSmallIcon(R.mipmap.ic_launcher)
                                                .setTimeoutAfter(1000L)
                                                .setContentTitle("Звонок с Домофона")
                                                .setFullScreenIntent(fullScreenPendingIntent, true)
                                                .setContentIntent(fullScreenPendingIntent)
                                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                                .setCategory(NotificationCompat.CATEGORY_ALARM)
                                                .build()

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                val channel = NotificationChannel(
                                                    channelId,
                                                    "LockScreenNotification",
                                                    NotificationManager.IMPORTANCE_HIGH
                                                ).apply {
                                                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                                                }
                                                nm.createNotificationChannel(channel)
                                            }
                                            nm.notify(id, notification)
                                        } else {
                                            sendCallNotification(data, service, preferenceStorage)
                                        }
                                    } else {
                                        val fullScreenIntent =
                                            Intent(service, IncomingCallActivity::class.java)
                                        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        fullScreenIntent.putExtra(FCM_DATA, data)
                                        service.startActivity(fullScreenIntent)
                                    }
                                    startRinging()
                                }
                            }

                        }catch (e: Throwable){
                            Timber.e("EXCEPTION LINPHONE_PROVIDER")
                            Timber.e(e)
                        }
                    }

                    CallStateSimple.END,
                    CallStateSimple.ERROR -> {
                        service.stopSelf()
                    }

                    CallStateSimple.CONNECTED -> {
                        deleteCallNotifications(service)
                        stopRinging()
                    }

                    CallStateSimple.OTHER_CONNECTED,
                    CallStateSimple.IDLE,
                    CallStateSimple.CONNECTING -> {
                    }
                }
            }
            super.onCallStateChanged(core, call, state, message)
        }
    }


    private fun wakeLock() {
        val powerManager =
            service.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "onCallStateChanged:centra.dom"
        )
        CoroutineScope(Dispatchers.Default).launch {
            wakeLock.acquire(1 * 60 * 1000L /*1 minutes*/)
            delay(3000)
            wakeLock.release()
        }
    }

    fun setNativeVideoWindowId(videoWindow: TextureView) {
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

    fun listenAndGetNotifications(pendingData: FcmCallData) {
        val ring = SoundChooser.getChosenTone(
            service,
            RingtoneManager.TYPE_RINGTONE,
            pendingData.flatId,
            preferenceStorage
        )

        Timber.d("debug_dmm ring.uri: ${ring.uri}")

        currentRingtone = RingtoneManager.getRingtone(service, ring.uri)
        fcmData = pendingData
        connect(pendingData)
    }

    private fun startRinging() {
        currentRingtone?.play()
        VibratorSingleton.apply {
            getVibrator(service)
            vibrateWithMode()
        }
    }

    fun stopRinging() {
        currentRingtone?.stop()
        VibratorSingleton.cancel()
    }

    fun acceptCall(videoWindow: TextureView) {
        core.currentCall?.let { call ->
            val params = core.createCallParams(call)
            params?.let {
                it.isVideoEnabled = true
                it.isAudioEnabled = true
            }
            call.acceptWithParams(params)
        }
    }

    fun acceptCallForDoor() {
        core.currentCall?.let { call ->
            val params = core.createCallParams(call)
            params?.let {
                it.isVideoEnabled = false
                it.isAudioEnabled = true
            }
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
        deleteCallNotifications(service)
        disconnect()
        finishCallActivity.value = Event(Unit)
    }

    private fun connect(data: FcmCallData) {
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
                val cfg = config.setAccount(mAccountCreator).createAccountInCore()
                val proxyConfig = cfg?.core?.createProxyConfig()
                proxyConfig?.let {
                     core.addProxyConfig(it)
                 }
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
        Timber.d("debug_dmm sending dtmf..")
        core.currentCall?.run {
            doDelayed(
                {
                    fcmData?.dtmf?.let { sendDtmfs(it) }
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

    companion object {
        const val NO_VIDEO_MODE = "StaticImage: Static picture"
        const val H264 = "H264"
    }
}

class SipConfig(
    var server: String = DataModule.URL,
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
        Call.State.OutgoingEarlyMedia -> {
            CallStateSimple.OTHER_CONNECTED
        }

        Call.State.PushIncomingReceived -> {
            CallStateSimple.INCOMING
        }
    }
}

enum class CallStateSimple {
    CONNECTED, CONNECTING, ERROR, IDLE, INCOMING, END, OTHER_CONNECTED
}
