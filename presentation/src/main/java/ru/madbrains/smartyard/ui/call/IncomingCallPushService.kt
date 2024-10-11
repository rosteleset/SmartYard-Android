package ru.madbrains.smartyard.ui.call

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ru.madbrains.domain.utils.doDelayed
import ru.madbrains.smartyard.LinphoneProvider
import ru.madbrains.smartyard.LinphoneService
import ru.madbrains.smartyard.VibratorSingleton


const val TAG = "IncomingCallPushReceiver"

class IncomingCallPushService : Service() {
    private lateinit var mLinphone: LinphoneProvider


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val provider = LinphoneService.instance?.provider
        if (provider != null) {
            mLinphone = provider
            if (intent?.action == OPEN_DOOR) {
                mLinphone.acceptCallForDoor()
                mLinphone.sendDtmf()

                doDelayed({
                    mLinphone.onDestroy()
                }, 3500)
            }
            if (intent?.action == IGNORE_CALL) {
                stopSelf()
            }

            VibratorSingleton.cancel()
        }
        doDelayed({
            stopSelf()
        }, 7000)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
     if (::mLinphone.isInitialized) {
         mLinphone.onDestroy()
     }

    }


    companion object {
        const val OPEN_DOOR = "OPEN_DOOR_NOTIFICATION"
        const val IGNORE_CALL = "IGNORE_CALL_NOTIFICATION"
    }
}


