/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.madbrains.smartyard

import android.Manifest
import android.app.Activity
import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.squareup.moshi.Moshi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.madbrains.data.DataModule
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.interactors.InboxInteractor
import ru.madbrains.domain.model.FcmCallData
import ru.madbrains.smartyard.ui.SoundChooser
import ru.madbrains.smartyard.ui.call.IncomingCallActivity.Companion.NOTIFICATION_ID
import ru.madbrains.smartyard.ui.getChannelId
import ru.madbrains.smartyard.ui.main.ChatWoot.ChatWootFragment
import ru.madbrains.smartyard.ui.main.MainActivity
import ru.madbrains.smartyard.ui.main.MainActivity.Companion.REQUEST_PERMISSION
import ru.madbrains.smartyard.ui.main.address.AddressComposeFragment.Companion.BROADCAST_CONFIRME_STATUS_PAY
import ru.madbrains.smartyard.ui.main.intercom.IntercomWebViewFragment.Companion.SET_COUNT_EVENTS
import ru.madbrains.smartyard.ui.main.notification.NotificationFragment.Companion.BROADCAST_ACTION_NOTIF
import ru.madbrains.smartyard.ui.main.pay.PayAddressFragment.Companion.BROADCAST_PAY_UPDATE
import ru.madbrains.smartyard.ui.reg.sms.PushRegFragment.Companion.BROADCAST_CONFIRM_CODE
import ru.madbrains.smartyard.ui.reg.sms.PushRegFragment.Companion.BROADCAST_REJECT_CODE
import ru.madbrains.smartyard.ui.reg.sms.PushRegFragment.Companion.INTENT_AUTHORIZATION_FAILED
import timber.log.Timber
import java.util.concurrent.Executors


const val TAG = "notification"

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val _jsonData = MutableLiveData<String>()
    val jsonData: LiveData<String>
        get() = _jsonData


    override fun doWork(): Result {
        val json = inputData.getString("yard")
        _jsonData.postValue(json.toString())
        // Выполните передачу данных или обработку здесь
        Timber.d("Notification Worker $json")
        val intentBroadcast = Intent(SET_COUNT_EVENTS)
        intentBroadcast.putExtra("yard", json)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentBroadcast)

        return Result.success()
    }

//    override fun getForegroundInfo(): ForegroundInfo {
//        return super.getForegroundInfo()
//    }

}

class FirebaseMessagingService : FirebaseMessagingService(), KoinComponent {
    private var mHandler = Handler(Looper.getMainLooper())
    private val preferenceStorage: PreferenceStorage by inject()
    private val mInteractor: AuthInteractor by inject()
    private val moshi: Moshi by inject()
    private val inboxInteractor: InboxInteractor by inject()
    private val context: Context get() = this


    override fun onCreate() {
        super.onCreate()

        FirebaseMessaging.getInstance().isAutoInitEnabled = false;
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.tag(TAG)
            .d("debug_dmm remoteMessage: from ${remoteMessage.from}; ttl = ${remoteMessage.ttl}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Timber.tag(TAG).d("debug_dmm Message Notification Body: ${it.body}")
        }
        if (remoteMessage.data.isNotEmpty()) {
            handleNow(remoteMessage)
        }

    }


    private fun handleNow(remoteMessage: RemoteMessage) {
        remoteMessage.data.let { data: MutableMap<String, String> ->
            Timber.tag(TAG).d("debug_dmm push: $data")
            with(data) {
                when {
                    get("action") == "chat" -> {
                        val intentBroadcast = Intent(ChatWootFragment.BROADCAST_MESSAGE_UPDATE)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBroadcast)
                        val messageId = get("messageId")
                        val messageType = get("messageType")

                        if ((application as? App)?.isChatActive == false) {
                            val badge = get("badge")?.toInt()
                            val title = remoteMessage.data.get("title")
                            val message = remoteMessage.data.get("body")

                            sendNotificationInbox(
                                messageId ?: "",
                                title ?: "",
                                message ?: "",
                                messageType ?: "",
                                badge ?: 0,
                                true
                            )
                        } else {

                        }
                    }


                    get("action") == "newAddress" -> {
                        val intentBroadcast = Intent(MainActivity.BROADCAST_LIST_UPDATE)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBroadcast)

                        val messageId = get("messageId")
                        val messageType = "inbox"
                        val badge = get("badge")?.toInt()
                        val title = remoteMessage.notification?.title
                        val message = remoteMessage.notification?.body
                        GlobalScope.launch {
                            Timber.tag(TAG).d("delivered run: %s", messageId)
                            messageId?.let {
                                inboxInteractor.delivered(it)
                            }
                        }

                        sendNotificationInbox(
                            messageId ?: "",
                            title ?: "",
                            message ?: "",
                            messageType ?: "",
                            badge ?: 0
                        )
                    }

                    containsKey("server") -> {
                        val json = JSONObject(data as Map<*, *>).toString()
                        Timber.tag(TAG).d("debug_dmm incoming call json: $json")
                        moshi.adapter(FcmCallData::class.java).fromJson(json)?.let { msg ->
                            msg.baseUrl?.let { baseUrl ->
                                if (baseUrl.isNotEmpty()) {
                                    preferenceStorage.baseUrl = baseUrl
                                    DataModule.URL = baseUrl
                                }
                            }

                            waitForLinServiceAndRun(msg) {
                                it.listenAndGetNotifications(msg)
                            }
                        }
                    }

                    get("action") == "inbox" || get("action") == "videoReady" -> {
                        val title: String
                        val message: String
                        val messageId = get("messageId")
                        val messageType = "inbox"
                        val badge = get("badge")?.toInt()

                        if (get("action") == "videoReady") {
                            title = remoteMessage.notification?.title.toString()
                            message = remoteMessage.notification?.body.toString()
                        } else {
                            title = remoteMessage.data["title"].toString()
                            message = remoteMessage.data["body"].toString()
                        }

                        GlobalScope.launch {
                            Timber.tag(TAG).d("delivered run: %s", messageId)
                            messageId?.let {
                                inboxInteractor.delivered(it)
                            }
                        }
                        sendNotificationInbox(
                            messageId ?: "",
                            title ?: "",
                            message ?: "",
                            messageType ?: "",
                            badge ?: 0
                        )
                    }

                    get("action") == "paySuccess" -> {
                        val intentBroadcast = Intent(BROADCAST_PAY_UPDATE)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBroadcast)

                        val messageId = get("messageId")
                        val messageType = get("messageType")
                        val badge = get("badge")?.toInt()
                        val title = remoteMessage.notification?.title
                        val message = remoteMessage.notification?.body
                        GlobalScope.launch {
                            Timber.tag(TAG).d("delivered run: %s", messageId)
                            messageId?.let {
                                inboxInteractor.delivered(it)
                            }
                        }
                        sendNotificationInbox(
                            messageId ?: "",
                            title ?: "",
                            message ?: "",
                            messageType ?: "",
                            badge ?: 0
                        )
                    }

                    get("action") == "updateStatusPay" -> {
                        val status = remoteMessage.data["status"] ?: ""
                        val contractTitle = remoteMessage.data["contractTitle"] ?: ""
                        if (status.isNotEmpty()) {
                            val intent = Intent(BROADCAST_CONFIRME_STATUS_PAY)
                            intent.putExtra("status", status)
                            intent.putExtra("contractTitle", contractTitle)
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                        } else {
                        }
                    }

                    get("action") == "authorization" -> {
                        Timber.d("PUSH action_authorization ${remoteMessage.data}")
                        val accessToken = remoteMessage.data["accessToken"] ?: ""
                        val name = remoteMessage.data["name"] ?: ""
                        val patronymic = remoteMessage.data["patronymic"] ?: ""

                        if (accessToken.isNotEmpty()) {
                            preferenceStorage.authToken = accessToken
                            val intentBroadcast = Intent(BROADCAST_CONFIRM_CODE)
                            intentBroadcast.putExtra("name", name)
                            intentBroadcast.putExtra("patronymic", patronymic)
                            intentBroadcast.putExtra("accessToken", accessToken)
                            LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(intentBroadcast)
                        } else {
                            //TODO redirect to sms code
                            val intentBroadcast = Intent(BROADCAST_REJECT_CODE)
                            LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(intentBroadcast)
                        }
                    }

                    get("action") == "authorizationFail" -> {
                        val intentBroadcast = Intent(INTENT_AUTHORIZATION_FAILED)
                        intentBroadcast.putExtra("authorizationFail", true)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBroadcast)
                    }

                    get("action") == "javascript" -> {
                        val json = remoteMessage.data["yard"]
                        if (!json.isNullOrEmpty()) {
                            val intentBroadcast = Intent(SET_COUNT_EVENTS)
                            intentBroadcast.putExtra("yard", json)
                            LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(intentBroadcast)

//                            val inputData = Data.Builder()
//                                .putString("yard", json)
//                                .build()
//
//                            val workRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
//                                .setInputData(inputData)
//                                .build()
//                            WorkManager.getInstance(context).enqueue(workRequest)
                        } else {

                        }
                    }

                    else -> {
                    }
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        Timber.d("debug_dmm new fcm token: $token")
        preferenceStorage.fcmToken = token

        if (preferenceStorage.authToken != null) {
            GlobalScope.launch {
                Timber.d("debug_dmm register fcm token: $token")
                mInteractor.registerPushToken(token)
                preferenceStorage.fcmTokenRegistered = token
            }
        }
    }

    private fun sendNotificationInbox(
        messageId: String,
        title: String,
        message: String,
        messageType: String,
        badge: Int,
        isChat: Boolean = false,
    ) {
        Timber.d("debug_dmm __Notification__")
        preferenceStorage.notificationData.addInboxNotification(preferenceStorage)
        val notId = preferenceStorage.notificationData.currentInboxId

        val tone = SoundChooser.getChosenTone(
            this,
            RingtoneManager.TYPE_NOTIFICATION,
            null,
            preferenceStorage
        )
        val soundUri = tone.uri

        Timber.d("debug_dmm soundUri: $soundUri")

        val channelId = getChannelId(tone.getToneTitle(this))

        val intent = Intent(this, MainActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.putExtra(NOTIFICATION_MESSAGE_ID, messageId)
        intent.putExtra(NOTIFICATION_MESSAGE_TYPE, TypeMessage.getTypeMessage(messageType))
        intent.putExtra(NOTIFICATION_ID, notId)

        val intentBroadcast = Intent(BROADCAST_ACTION_NOTIF)
        intentBroadcast.putExtra(NOTIFICATION_MESSAGE_ID, messageId)
        intentBroadcast.putExtra(NOTIFICATION_ID, notId)
        intentBroadcast.putExtra(NOTIFICATION_BADGE, badge)

        if (isChat) {
            intent.action = NOTIFICATION_CHAT
            intentBroadcast.action = NOTIFICATION_CHAT
            intentBroadcast.addCategory(Intent.CATEGORY_LAUNCHER)
            intentBroadcast.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intentBroadcast.putExtra(NOTIFICATION_CHAT, true)
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast)

        val pendingIntent = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) //TODO ic_launcher_round
            .setContentTitle(title)
            .setContentText(message)
            .setSound(soundUri)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)


        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "InboxMessagesChannel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            channel.setSound(soundUri, audioAttributes)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notId, notificationBuilder.build())

    }


    companion object {
        const val TITLE = "title"
        const val NOTIFICATION_MESSAGE_ID = "messageId"
        const val NOTIFICATION_MESSAGE_TYPE = "messageType"
        const val NOTIFICATION_BADGE = "badge"
        const val NOTIFICATION_CHAT = "chat"
        const val CALL_STUN = "stun"
        const val CALL_STUN_TRANSPORT = "stn_transport"
        const val CALL_TURN_USERNAME = "turn_username"
        const val CALL_TURN_PASSWORD = "turn_password"
    }

    enum class TypeMessage {
        CHAT, INBOX, NO_DEFINE;

        companion object {
            fun getTypeMessage(type: String): TypeMessage {
                return when (type) {
                    "inbox" -> INBOX
                    "chat" -> CHAT
                    else -> NO_DEFINE
                }
            }
        }
    }


    private fun waitForLinServiceAndRun(
        fcmCallData: FcmCallData,
        listener: (LinphoneProvider) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            val intent = Intent(REQUEST_PERMISSION)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            if (!LinphoneService.isReady()) {
                val intent = Intent().setClass(context, LinphoneService::class.java).apply {
                    if (fcmCallData.stun?.isNotEmpty() == true) {
                        putExtra(CALL_STUN, fcmCallData.stun)
                        putExtra(CALL_STUN_TRANSPORT, fcmCallData.stun_transport ?: "udp")
                        putExtra(CALL_TURN_USERNAME, fcmCallData.extension)
                        putExtra(CALL_TURN_PASSWORD, fcmCallData.pass)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
            while (!LinphoneService.isReady()) {
                try {
                    Thread.sleep(30)
                } catch (e: InterruptedException) {
                    throw RuntimeException("waiting thread sleep() has been interrupted")
                }
            }
            mHandler.post { LinphoneService.instance?.provider?.let { listener(it) } }
        }
    }
}