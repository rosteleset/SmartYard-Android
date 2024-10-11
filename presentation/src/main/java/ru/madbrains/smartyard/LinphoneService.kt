package ru.madbrains.smartyard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import org.koin.core.component.KoinComponent
import org.linphone.core.Core
import org.linphone.core.Factory
import org.linphone.core.LogCollectionState
import org.linphone.core.tools.Log
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*


class LinphoneService : Service(), KoinComponent {
    private var mCore: Core? = null
    var provider: LinphoneProvider? = null
    private val mTaskHandler = Handler(Looper.getMainLooper())
    private var mTimer = Timer()
    private var intentVolume = Intent()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        intentVolume = Intent(this, VolumeButtonService::class.java)
        startService(intentVolume)

        createCallChannel()

        Timber.d("debug_dmm LinphoneService create")
        val basePath = filesDir.absolutePath
        try {
            copyIfNotExist(R.raw.linphonerc_default, "$basePath/$FILE")
            copyFromPackage(R.raw.linphonerc_factory, FOLDER)
        } catch (ioe: IOException) {
            Log.e(ioe)
        }

        //Factory.instance().setDebugMode(true, "__Q__")

        val core = Factory.instance().createCore("$basePath/$FILE", "$basePath/$FOLDER", this)
        mCore = core
        provider = LinphoneProvider(core, this)
        core.transportsUsed.run {
            Timber.i("debug_dmm linphone ports:\ntls: $tlsPort udp: $udpPort tcp:$tcpPort")
        }
    }

    private fun createCallChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("debug_dmm __Notification__")
            val id = (1000..1000000).random()
            val CHANNEL_ID = "CallChannel"

            val channel = NotificationChannel(
                CHANNEL_ID,
                "CallChannelNotification",
                NotificationManager.IMPORTANCE_DEFAULT  //NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentTitle("Звонок")
                .setContentText("Вам звонит домофон")
                .build()

            startForeground(id, notification)
        }
    }

    override fun onDestroy() {
        Timber.d("debug_dmm Linphone stopping... >>>")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibrator = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator.cancel()
        } else {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        }

        VibratorSingleton.cancel()
        provider?.onDestroy()
        mTimer.cancel()
        mCore?.stop()
        mCore = null
        instance = null
        stopService(intentVolume)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("debug_dmm Linphone starting... >>>")
        mCore?.let { core ->
            //ICE/STUN/TURN
            var nat = core.natPolicy
            if (nat == null) {
                nat = core.createNatPolicy()
            }

            var stun = intent?.getStringExtra(FirebaseMessagingService.CALL_STUN) ?: ""
            val turnTransport =
                intent?.getStringExtra(FirebaseMessagingService.CALL_STUN_TRANSPORT) ?: ""
            val turnUsername =
                intent?.getStringExtra(FirebaseMessagingService.CALL_TURN_USERNAME) ?: ""
            val turnPassword =
                intent?.getStringExtra(FirebaseMessagingService.CALL_TURN_PASSWORD) ?: ""
            Timber.d("debug_dmm Linphone stun:$stun")
            if (stun.startsWith("turn:")) {
                stun = stun.substring(5)
                nat.apply {
                    isStunEnabled = false
                    isIceEnabled = true
                    isTurnEnabled = true
                }
                nat.stunServer = stun
                nat.stunServerUsername = turnUsername

                when (turnTransport.toLowerCase(Locale.getDefault())) {
                    "tcp" -> {
                        nat.apply {
                            isUdpTurnTransportEnabled = false
                            isTcpTurnTransportEnabled = true
                            isTlsTurnTransportEnabled = false
                        }
                    }
                    "tls" -> {
                        nat.apply {
                            isUdpTurnTransportEnabled = false
                            isTcpTurnTransportEnabled = false
                            isTlsTurnTransportEnabled = true
                        }
                    }
                    else -> {
                        nat.apply {
                            isUdpTurnTransportEnabled = true
                            isTcpTurnTransportEnabled = false
                            isTlsTurnTransportEnabled = false
                        }
                    }
                }

                var authInfo = core.findAuthInfo(null, turnUsername, null)
                if (authInfo != null) {
                    val cloneAuthInfo = authInfo.clone()
                    core.removeAuthInfo(authInfo)
                    cloneAuthInfo.password = turnPassword
                    core.addAuthInfo(cloneAuthInfo)
                } else {
                    authInfo = Factory.instance()
                        .createAuthInfo(turnUsername, turnUsername, turnPassword, null, null, null)
                    core.addAuthInfo(authInfo)
                }

                Timber.d("debug_dmm Linphone is using turn $stun")
            } else if (stun.startsWith("stun:")) {
                stun = stun.substring(5)
                nat.apply {
                    isStunEnabled = true
                    isIceEnabled = true
                    isTurnEnabled = false
                }
                nat.stunServer = stun
                Timber.d("debug_dmm Linphone is using stun $stun")
            }
            if (stun.isEmpty()) {
                Timber.d("debug_dmm Linphone is not using stun")
            }
            core.natPolicy = nat
        }

        if (instance != null) {
            return START_STICKY
        }
        mCore?.let { core ->
            core.start()
            Factory.instance()
                .enableLogCollection(LogCollectionState.EnabledWithoutPreviousLogHandler)

            Factory.instance().loggingService.addListener { logService, domain, level, message ->
                Timber.i("debug_dmm message: $message")
            }

            core.clearAllAuthInfo()
            core.clearProxyConfig()
        }
        val lTask = object : TimerTask() {
            override fun run() {
                mTaskHandler.post {
                    mCore?.iterate()
                }
            }
        }
        mTimer = Timer("Linphone scheduler")
        mTimer.schedule(lTask, 0, 20)
        instance = this
        return START_STICKY
    }

    @Throws(IOException::class)
    private fun copyIfNotExist(ressourceId: Int, target: String) {
        val lFileToCopy = File(target)
        if (!lFileToCopy.exists()) {
            copyFromPackage(ressourceId, lFileToCopy.name)
        }
    }

    @Throws(IOException::class)
    private fun copyFromPackage(ressourceId: Int, target: String) {
        val lOutputStream = openFileOutput(target, 0)
        val lInputStream = resources.openRawResource(ressourceId)
        val buff = ByteArray(8048)
        var readByte = 0
        while ({ readByte = lInputStream.read(buff); readByte }() != -1) {
            lOutputStream.write(buff, 0, readByte)
        }
        lOutputStream.flush()
        lOutputStream.close()
        lInputStream.close()
    }

    companion object {
        const val FOLDER = "linphonerc"
        const val FILE = ".linphonerc"
        var instance: LinphoneService? = null
        fun isReady(): Boolean {
            return instance != null
        }

        const val RANDOM_PORT = -1
    }
}