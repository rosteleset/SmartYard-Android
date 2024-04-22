package com.sesameware.smartyard_oem

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import org.linphone.core.Core
import org.linphone.core.Factory
import org.linphone.core.LogLevel
import org.linphone.core.tools.Log
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

class LinphoneService : Service() {
    var mCore: Core? = null
    var provider: LinphoneProvider? = null
    private val mTaskHandler = Handler()
    private var mTimer = Timer()
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("debug_dmm LinphoneService create")
        val basePath = filesDir.absolutePath
        try {
            copyIfNotExist(R.raw.linphonerc_default, "$basePath/$FILE")
            copyFromPackage(R.raw.linphonerc_factory, FOLDER)
        } catch (ioe: IOException) {
            Log.e(ioe)
        }

        /*Factory.instance().setLoggerDomain("__L__")
        Factory.instance().enableLogcatLogs(true)
        Factory.instance().loggingService.setLogLevel(LogLevel.Message)*/

        val core = Factory.instance().createCore("$basePath/$FILE", "$basePath/$FOLDER", this)
        core.setUserAgent("Teledom", BuildConfig.VERSION_NAME)
        mCore = core
        provider = LinphoneProvider(core, this)
        core.transportsUsed.run {
            Timber.i("debug_dmm linphone ports:\ntls: $tlsPort udp: $udpPort tcp:$tcpPort")
        }
        //mCore?.micGainDb = 5.0f
    }

    override fun onDestroy() {
        Timber.d("debug_dmm Linphone stopping... >>>")
        provider?.onDestroy()
        mTimer.cancel()
        mCore?.stop()
        mCore = null
        instance = null
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
            val turnTransport = intent?.getStringExtra(FirebaseMessagingService.CALL_STUN_TRANSPORT) ?: "udp"
            val turnUsername = intent?.getStringExtra(FirebaseMessagingService.CALL_TURN_USERNAME) ?: ""
            val turnPassword = intent?.getStringExtra(FirebaseMessagingService.CALL_TURN_PASSWORD) ?: ""

            if (stun.startsWith("turn:")) {
                stun = stun.substring(5)
                nat.isStunEnabled = false
                nat.isIceEnabled = true
                nat.isTurnEnabled = true
                nat.stunServer = stun
                nat.stunServerUsername = turnUsername

                when (turnTransport.lowercase(Locale.getDefault())) {
                    "tcp" -> {
                        nat.isUdpTurnTransportEnabled = false
                        nat.isTcpTurnTransportEnabled = true
                        nat.isTlsTurnTransportEnabled = false
                    }
                    "tls" -> {
                        nat.isUdpTurnTransportEnabled = false
                        nat.isTcpTurnTransportEnabled = false
                        nat.isTlsTurnTransportEnabled = true
                    }
                    else -> {
                        nat.isUdpTurnTransportEnabled = true
                        nat.isTcpTurnTransportEnabled = false
                        nat.isTlsTurnTransportEnabled = false
                    }
                }

                var authInfo = core.findAuthInfo(null, turnUsername, null)
                if (authInfo != null) {
                    val cloneAuthInfo = authInfo.clone()
                    core.removeAuthInfo(authInfo)
                    cloneAuthInfo.password = turnPassword
                    core.addAuthInfo(cloneAuthInfo)
                } else {
                    authInfo = Factory.instance().createAuthInfo(turnUsername, turnUsername, turnPassword, null, null, null)
                    core.addAuthInfo(authInfo)
                }

                Timber.d("debug_dmm Linphone is using turn $stun")
            } else if (stun.startsWith("stun:")) {
                stun = stun.substring(5)
                nat.isStunEnabled = true
                nat.isIceEnabled = true
                nat.isTurnEnabled = false
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
    }
}
