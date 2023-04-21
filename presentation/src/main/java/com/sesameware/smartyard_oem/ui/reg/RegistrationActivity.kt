package com.sesameware.smartyard_oem.ui.reg

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.core.view.ViewCompat
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.FirebaseMessagingService
import com.sesameware.smartyard_oem.FirebaseMessagingService.TypeMessage
import com.sesameware.smartyard_oem.FirebaseMessagingService.TypeMessage.Companion.getTypeMessage
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ActivityRegistrationBinding
import com.sesameware.smartyard_oem.reduceToZero
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity.Companion.NOTIFICATION_ID
import com.sesameware.smartyard_oem.ui.getBottomNavigationHeight
import timber.log.Timber

class RegistrationActivity : CommonActivity() {
    private lateinit var binding: ActivityRegistrationBinding

    override val mViewModel by viewModel<RegistrationViewModel>()
    private var messageId = ""
    private var messageType = TypeMessage.NO_DEFINE
    private var notificationId = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)

        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        Timber.d("debug_dmm    in onCreate")
        intent?.extras?.let {
            Timber.d("debug_dmm    has extras")
            intentParse(it)
        }

        mViewModel.onStart(supportFragmentManager.findFragmentById(R.id.navFragment)!!, messageId, messageType, activity = this@RegistrationActivity)

        val bottomNavHeight = getBottomNavigationHeight(this@RegistrationActivity)
        ViewCompat.setOnApplyWindowInsetsListener(binding.frameLayout) { _, insets ->
            ViewCompat.onApplyWindowInsets(
                binding.frameLayout,
                insets.replaceSystemWindowInsets(
                    insets.systemWindowInsetLeft, 0,
                    insets.systemWindowInsetRight,
                    (insets.systemWindowInsetBottom - bottomNavHeight).reduceToZero()
                )
            )
        }
    }

    private fun intentParse(bundle: Bundle) {
        Timber.d("debug_dmm   intentParse")
        messageId = bundle.getString(FirebaseMessagingService.NOTIFICATION_MESSAGE_ID, "")
        messageType =
            getTypeMessage(
                bundle.getString(
                    FirebaseMessagingService.NOTIFICATION_MESSAGE_TYPE,
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
