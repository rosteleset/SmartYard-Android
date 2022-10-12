package ru.madbrains.smartyard.ui.launcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.CommonActivity
import ru.madbrains.smartyard.EventObserver
import ru.madbrains.smartyard.FirebaseMessagingService
import ru.madbrains.smartyard.ui.onboarding.OnboardingActivity
import ru.madbrains.smartyard.ui.reg.RegistrationActivity

class LauncherActivity : CommonActivity() {

    override val mViewModel by viewModel<LauncherViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
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
        val intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelMessages = NotificationChannel(
                FirebaseMessagingService.CHANNEL_INBOX_ID,
                FirebaseMessagingService.CHANNEL_INBOX_TITLE,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channelMessages)

            val channelCalls = NotificationChannel(
                FirebaseMessagingService.CHANNEL_CALLS_ID,
                FirebaseMessagingService.CHANNEL_CALLS_TITLE,
                NotificationManager.IMPORTANCE_HIGH
            )

            //задаём шаблон вибрации
            channelCalls.vibrationPattern = FirebaseMessagingService.CALL_VIBRATION_PATTERN

            //отключаем звук уведомления, так как он запускается при успешном sip соединении
            channelCalls.setSound(null, null)

            notificationManager.createNotificationChannel(channelCalls)
        }
    }
}
