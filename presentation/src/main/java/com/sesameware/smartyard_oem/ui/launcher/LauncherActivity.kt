package com.sesameware.smartyard_oem.ui.launcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.EventObserver
import com.sesameware.smartyard_oem.MessagingService
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.onboarding.OnboardingActivity
import com.sesameware.smartyard_oem.ui.reg.RegistrationActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class LauncherActivity : CommonActivity() {
    override val mViewModel by viewModel<LauncherViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }

        super.onCreate(savedInstanceState)

        createNotificationChannels()

        mViewModel.launchDestination.observe(
            this,
            EventObserver { destination ->
                when (destination) {
                    LauncherViewModel.LaunchDestination.ONBOARDING_ACTIVITY -> openOnboardingActivity()
                    LauncherViewModel.LaunchDestination.REGISTRATION_ACTIVITY -> openRegistrationActivity()
                }
            }
        )
    }

    private fun openOnboardingActivity() {
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openRegistrationActivity() {
        Timber.d("debug_dmm   openRegistrationActivity   ${intent?.extras?.keySet()?.joinToString(", ")}")
        val regIntent = Intent(this, RegistrationActivity::class.java)
        if (intent.extras?.containsKey("action") == true) {
            regIntent.putExtra(MessagingService.NOTIFICATION_MESSAGE_TYPE, intent.extras?.getString("action"))
        }
        if (intent.extras?.containsKey(MessagingService.NOTIFICATION_MESSAGE_ID) == true) {
            regIntent.putExtra(MessagingService.NOTIFICATION_MESSAGE_ID, intent.extras?.getString(MessagingService.NOTIFICATION_MESSAGE_ID))
        }
        startActivity(regIntent)
        finish()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelMessages = NotificationChannel(
                MessagingService.CHANNEL_INBOX_ID,
                this.getString(R.string.channel_inbox_title),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channelMessages)

            val channelCalls = NotificationChannel(
                MessagingService.CHANNEL_CALLS_ID,
                this.getString(R.string.channel_calls_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
            }

            //включаем вибрацию
            channelCalls.enableVibration(true)
            channelCalls.vibrationPattern = MessagingService.CALL_VIBRATION_PATTERN

            //отключаем звук уведомления, так как он запускается при успешном sip соединении
            channelCalls.setSound(null, null)

            notificationManager.createNotificationChannel(channelCalls)
        }
    }
}
