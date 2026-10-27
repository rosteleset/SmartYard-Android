package com.sesameware.smartyard_oem.ui.reg

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.sesameware.domain.interfaces.NetworkMonitor
import com.sesameware.domain.model.NetworkStatus
import com.sesameware.smartyard_oem.CommonActivity
import com.sesameware.smartyard_oem.MessagingService
import com.sesameware.smartyard_oem.MessagingService.TypeMessage
import com.sesameware.smartyard_oem.MessagingService.TypeMessage.Companion.getTypeMessage
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.ActivityRegistrationBinding
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity.Companion.NOTIFICATION_ID
import com.sesameware.smartyard_oem.ui.main.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.fragment.android.setupKoinFragmentFactory
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class RegistrationActivity : CommonActivity() {
    private lateinit var binding: ActivityRegistrationBinding

    override val mViewModel by viewModel<RegistrationViewModel>()
    private var messageId = ""
    private var messageType = TypeMessage.NO_DEFINE
    private var notificationId = 0
    private val networkMonitor: NetworkMonitor by inject()
    private var startupDialog: AlertDialog? = null
    private val startupStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Undefined)
    private var providerStartupStarted = false
    private var offlineRoutePending = false
    private var mainLaunched = false

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

        // Cancellation stays active even while UI collection is paused. In particular,
        // a disconnect must win over provider responses waiting for the next resume.
        lifecycleScope.launch {
            networkMonitor.networkStatus.collect { status ->
                if (status == NetworkStatus.Undefined) return@collect
                if (status == NetworkStatus.Disconnected) {
                    mViewModel.cancelProviderRequests()
                    providerStartupStarted = false
                    if (!mViewModel.authToken.isNullOrBlank()) offlineRoutePending = true
                }
                startupStatus.value = status
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                startupStatus.collect { status ->
                    if (status == NetworkStatus.Undefined || !canUpdateStartupUi()) return@collect
                    if (offlineRoutePending) {
                        launchMainOffline()
                    } else if (status == NetworkStatus.Disconnected) {
                        showStartupUnavailable()
                    } else {
                        dismissStartupDialog()
                        val fragment = supportFragmentManager.findFragmentById(R.id.navFragment)
                            ?: return@collect
                        val onProviders = NavHostFragment.findNavController(fragment)
                            .currentDestination?.id == R.id.providersFragment
                        if (!providerStartupStarted && onProviders) {
                            providerStartupStarted = true
                            mViewModel.onStart(fragment, messageId, messageType, this@RegistrationActivity)
                        }
                    }
                }
            }
        }
    }

    private fun canUpdateStartupUi(): Boolean =
        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            !isFinishing && !isDestroyed && !supportFragmentManager.isStateSaved && !mainLaunched

    private fun launchMainOffline() {
        if (!canUpdateStartupUi()) return
        mViewModel.cancelProviderRequests()
        dismissStartupDialog()
        mainLaunched = true
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putExtra(MessagingService.NOTIFICATION_MESSAGE_ID, messageId)
                putExtra(MessagingService.NOTIFICATION_MESSAGE_TYPE, messageType)
            }
        )
        finish()
    }

    private fun dismissStartupDialog() {
        startupDialog?.dismiss()
        startupDialog = null
    }

    private fun showStartupUnavailable() {
        if (startupDialog != null || !canUpdateStartupUi()) return
        startupDialog = AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setMessage("Пользователь не зарегистрирован. Сеть недоступна. Нажмите ОК для выхода из приложения")
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ -> finishAffinity() }
            .show()
    }

    override fun onStop() {
        dismissStartupDialog()
        super.onStop()
    }

    private fun intentParse(bundle: Bundle) {
        Timber.d("debug_dmm   intentParse")
        messageId = bundle.getString(MessagingService.NOTIFICATION_MESSAGE_ID, "")
        @Suppress("DEPRECATION")
        val incomingType = bundle.getSerializable(MessagingService.NOTIFICATION_MESSAGE_TYPE)
        messageType = when (incomingType) {
            is TypeMessage -> incomingType
            is String -> getTypeMessage(incomingType)
            else -> TypeMessage.NO_DEFINE
        }
        notificationId = bundle.getInt(NOTIFICATION_ID, 0)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        Timber.tag(RegistrationActivity::class.simpleName)
            .d("Intent parse: %s %s %s", messageId, messageType, notificationId)
    }
}
