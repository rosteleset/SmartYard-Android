package com.sesameware.smartyard_oem.ui.reg

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.MessagingService
import com.sesameware.smartyard_oem.MessagingService.TypeMessage
import com.sesameware.smartyard_oem.MessagingService.TypeMessage.Companion.getTypeMessage
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ActivityRegistrationBinding
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity.Companion.NOTIFICATION_ID
import org.koin.androidx.fragment.android.setupKoinFragmentFactory
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class RegistrationActivity : CommonActivity() {
    private lateinit var binding: ActivityRegistrationBinding

    override val mViewModel by viewModel<RegistrationViewModel>()
    private var messageId = ""
    private var messageType = TypeMessage.NO_DEFINE
    private var notificationId = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        setupKoinFragmentFactory()

        setTheme(R.style.AppTheme_NoActionBar)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)

        fragmentHasHeader = true

        binding = ActivityRegistrationBinding.inflate(layoutInflater)

        setContentView(binding.root)

        Timber.d("debug_dmm    in onCreate")
        intent?.extras?.let {
            Timber.d("debug_dmm    has extras")
            intentParse(it)
        }

        mViewModel.onStart(supportFragmentManager.findFragmentById(R.id.navFragment)!!, messageId, messageType, activity = this@RegistrationActivity)


    }

    private fun intentParse(bundle: Bundle) {
        Timber.d("debug_dmm   intentParse")
        messageId = bundle.getString(MessagingService.NOTIFICATION_MESSAGE_ID, "")
        messageType =
            getTypeMessage(
                bundle.getString(
                    MessagingService.NOTIFICATION_MESSAGE_TYPE,
                    ""
                )
            )
        notificationId = bundle.getInt(NOTIFICATION_ID, 0)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        Timber.tag(RegistrationActivity::class.simpleName)
            .d("Intent parse: %s %s %s", messageId, messageType, notificationId)
    }
}
