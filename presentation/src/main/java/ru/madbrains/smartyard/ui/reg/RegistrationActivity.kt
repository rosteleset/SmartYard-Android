package ru.madbrains.smartyard.ui.reg

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.core.view.ViewCompat
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.madbrains.smartyard.CommonActivity
import ru.madbrains.smartyard.FirebaseMessagingService
import ru.madbrains.smartyard.FirebaseMessagingService.TypeMessage
import ru.madbrains.smartyard.FirebaseMessagingService.TypeMessage.Companion.getTypeMessage
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.ActivityRegistrationBinding
import ru.madbrains.smartyard.reduceToZero
import ru.madbrains.smartyard.ui.call.IncomingCallActivity.Companion.NOTIFICATION_ID
import ru.madbrains.smartyard.ui.getBottomNavigationHeight
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

        intent?.extras?.let {
            intentParse(it)
        }

        mViewModel.onStart(supportFragmentManager.findFragmentById(R.id.navFragment)!!, messageId, messageType, activity = this)

        val bottomNavHeight = getBottomNavigationHeight(this)
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
        println("TESTS")
        if (messageId.isNotEmpty()) {
            mViewModel.delivered(messageId)
        }

        Timber.tag(RegistrationActivity::class.simpleName)
            .d("Intent parse: %s %s %s", messageId, messageType, notificationId)
    }
}
